package com.riskregister.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RiskApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private static String risk(String category, String status, Object likelihood, Object impact) {
        return """
                {"title":"Data breach","description":"d","category":"%s","owner":"CISO",
                 "likelihood":%s,"impact":%s,"status":"%s"}""".formatted(category, likelihood, impact, status);
    }

    private long createRisk(String category, int l, int i) throws Exception {
        String body = mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content(risk(category, "OPEN", l, i)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private ResultActions addMitigation(long riskId, int effectiveness) throws Exception {
        return mvc.perform(post("/api/risks/{id}/mitigations", riskId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"MFA\",\"effectiveness\":" + effectiveness + "}"));
    }

    @Test
    void createRisk_addMitigation_residualScoreUpdates() throws Exception {
        long id = createRisk("SECURITY", 4, 5);

        mvc.perform(get("/api/risks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inherentScore", is(20)))
                .andExpect(jsonPath("$.inherentSeverity", is("CRITICAL")))
                .andExpect(jsonPath("$.residualScore", is(20)))   // no mitigations yet
                .andExpect(jsonPath("$.mitigations", hasSize(0)));

        addMitigation(id, 5).andExpect(status().isCreated());

        mvc.perform(get("/api/risks/{id}", id))
                .andExpect(jsonPath("$.residualScore", is(5)))
                .andExpect(jsonPath("$.residualSeverity", is("LOW")))
                .andExpect(jsonPath("$.mitigations", hasSize(1)));
    }

    @Test
    void invalidScaleValuesAreRejectedWithClearMessages() throws Exception {
        for (String bad : new String[] {"0", "6", "3.5", "\"x\"", "null"}) {
            mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                            .content(risk("SECURITY", "OPEN", bad, 3)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", is("likelihood")));
        }
        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content(risk("SECURITY", "OPEN", 3, 6)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("between 1 and 5")));

        long id = createRisk("SECURITY", 2, 2);
        addMitigation(id, 9).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("effectiveness")));
    }

    @Test
    void unknownCategoryIsA400ListingAllowedValues() throws Exception {
        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content(risk("NOPE", "OPEN", 3, 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("category")))
                .andExpect(jsonPath("$.errors[0].message", containsString("SECURITY")));
    }

    @Test
    void closingWithoutMitigationIs409_thenAllowedOnceMitigated_andLastMitigationCannotBeDeleted() throws Exception {
        long id = createRisk("COMPLIANCE", 3, 3);

        mvc.perform(put("/api/risks/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(risk("COMPLIANCE", "CLOSED", 3, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("at least one mitigation")));

        mvc.perform(post("/api/risks").contentType(MediaType.APPLICATION_JSON)
                        .content(risk("COMPLIANCE", "CLOSED", 3, 3)))
                .andExpect(status().isConflict());

        String m = addMitigation(id, 4).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long mitigationId = json.readTree(m).get("id").asLong();

        mvc.perform(put("/api/risks/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(risk("COMPLIANCE", "CLOSED", 3, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")));

        mvc.perform(delete("/api/risks/{r}/mitigations/{m}", id, mitigationId))
                .andExpect(status().isConflict());
    }

    @Test
    void listFiltersByCategoryAndStatus_andSortsByResidualDescending() throws Exception {
        // Unique category value per run is not possible, so assert relative order on the FINANCIAL subset.
        long highInherentButMitigated = createRisk("FINANCIAL", 5, 5);   // 25 -> 6 after e=5,e=5? see below
        addMitigation(highInherentButMitigated, 5).andExpect(status().isCreated());
        addMitigation(highInherentButMitigated, 5).andExpect(status().isCreated()); // 25 * .25 * .25 -> 1.56 -> capped 2.5 -> 3
        long mediumUnmitigated = createRisk("FINANCIAL", 3, 4);          // 12, unmitigated

        String body = mvc.perform(get("/api/risks").param("category", "FINANCIAL")
                        .param("status", "OPEN").param("sort", "residualScore,desc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode rows = json.readTree(body);
        int previous = Integer.MAX_VALUE;
        int posMedium = -1, posMitigated = -1;
        for (int i = 0; i < rows.size(); i++) {
            JsonNode row = rows.get(i);
            int residual = row.get("residualScore").asInt();
            org.assertj.core.api.Assertions.assertThat(residual).isLessThanOrEqualTo(previous);
            org.assertj.core.api.Assertions.assertThat(row.get("category").asText()).isEqualTo("FINANCIAL");
            previous = residual;
            if (row.get("id").asLong() == mediumUnmitigated) posMedium = i;
            if (row.get("id").asLong() == highInherentButMitigated) posMitigated = i;
        }
        org.assertj.core.api.Assertions.assertThat(posMedium).isGreaterThanOrEqualTo(0)
                .isLessThan(posMitigated); // residual 12 ranks above residual 3
        org.assertj.core.api.Assertions.assertThat(rows.get(posMitigated).get("mitigationCount").asInt()).isEqualTo(2);

        mvc.perform(get("/api/risks").param("sort", "bogus")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/risks").param("status", "WRONG")).andExpect(status().isBadRequest());
    }

    @Test
    void deletingARiskCascadesToItsMitigations() throws Exception {
        long id = createRisk("OPERATIONAL", 2, 3);
        addMitigation(id, 3).andExpect(status().isCreated());

        mvc.perform(delete("/api/risks/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/risks/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void mitigationOnAnotherRiskIsNotFound() throws Exception {
        long a = createRisk("STRATEGIC", 2, 2);
        long b = createRisk("STRATEGIC", 2, 2);
        String m = addMitigation(a, 2).andReturn().getResponse().getContentAsString();
        long mitigationId = json.readTree(m).get("id").asLong();

        mvc.perform(delete("/api/risks/{r}/mitigations/{m}", b, mitigationId)).andExpect(status().isNotFound());
        mvc.perform(put("/api/risks/{r}/mitigations/{m}", a, mitigationId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"better\",\"effectiveness\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveness", is(4)));
    }
}
