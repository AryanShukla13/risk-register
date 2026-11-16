package com.riskregister.risk;

import com.riskregister.common.BusinessRuleException;
import com.riskregister.common.NotFoundException;
import com.riskregister.risk.dto.RiskRequest;
import com.riskregister.risk.dto.RiskResponse;
import com.riskregister.risk.dto.RiskSummaryResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RiskService {

    static final String CLOSE_WITHOUT_MITIGATION =
            "A risk cannot be closed without at least one mitigation. Add a mitigation first.";

    private final RiskRepository risks;
    private final RiskMapper mapper;

    public RiskService(RiskRepository risks, RiskMapper mapper) {
        this.risks = risks;
        this.mapper = mapper;
    }

    public RiskResponse create(RiskRequest req) {
        // A brand-new risk has zero mitigations, so it can never be created as CLOSED.
        assertCanBeClosed(req.status(), 0);
        Risk risk = new Risk(req.title().strip(), req.description(), req.category(), req.owner().strip(),
                req.likelihood(), req.impact(), req.status());
        return mapper.toResponse(risks.save(risk));
    }

    @Transactional(readOnly = true)
    public RiskResponse get(long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<RiskSummaryResponse> list(RiskCategory category, RiskStatus status, RiskSort sort) {
        List<RiskSummaryResponse> rows = risks.findAll(RiskSpecifications.matching(category, status)).stream()
                .map(mapper::toSummary)
                .toList();
        return rows.stream().sorted(comparator(sort)).toList();
    }

    public RiskResponse update(long id, RiskRequest req) {
        Risk risk = find(id);
        assertCanBeClosed(req.status(), risk.getMitigations().size());
        risk.apply(req.title().strip(), req.description(), req.category(), req.owner().strip(),
                req.likelihood(), req.impact(), req.status());
        return mapper.toResponse(risks.saveAndFlush(risk)); // flush so updatedAt is current in the response
    }

    public void delete(long id) {
        risks.delete(find(id));
    }

    private Risk find(long id) {
        return risks.findWithMitigationsById(id)
                .orElseThrow(() -> new NotFoundException("Risk " + id + " not found"));
    }

    /** The single place the "no CLOSED without a mitigation" rule is expressed. */
    public static void assertCanBeClosed(RiskStatus status, int mitigationCount) {
        if (status == RiskStatus.CLOSED && mitigationCount == 0) {
            throw new BusinessRuleException(CLOSE_WITHOUT_MITIGATION);
        }
    }

    private static Comparator<RiskSummaryResponse> comparator(RiskSort sort) {
        return switch (sort) {
            case CREATED_DESC -> Comparator.comparing(RiskSummaryResponse::createdAt).reversed()
                    .thenComparing(Comparator.comparing(RiskSummaryResponse::id).reversed());
            case RESIDUAL_DESC -> Comparator.comparingInt(RiskSummaryResponse::residualScore).reversed()
                    .thenComparing(Comparator.comparingInt(RiskSummaryResponse::inherentScore).reversed())
                    .thenComparing(RiskSummaryResponse::id);
            case RESIDUAL_ASC -> Comparator.comparingInt(RiskSummaryResponse::residualScore)
                    .thenComparingInt(RiskSummaryResponse::inherentScore)
                    .thenComparing(RiskSummaryResponse::id);
        };
    }
}
