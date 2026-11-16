package com.riskregister.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.riskregister.common.BusinessRuleException;
import com.riskregister.common.NotFoundException;
import com.riskregister.mitigation.Mitigation;
import com.riskregister.risk.dto.RiskRequest;
import com.riskregister.scoring.DiminishingReturnsResidualCalculator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiskServiceTest {

    private RiskRepository repo;
    private RiskService service;

    @BeforeEach
    void setUp() {
        repo = mock(RiskRepository.class);
        service = new RiskService(repo, new RiskMapper(new DiminishingReturnsResidualCalculator()));
    }

    private static RiskRequest request(RiskStatus status) {
        return new RiskRequest("Title", "desc", RiskCategory.SECURITY, "owner", 4, 5, status);
    }

    @Test
    void cannotCreateAClosedRisk() {
        assertThatThrownBy(() -> service.create(request(RiskStatus.CLOSED)))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void cannotCloseARiskWithZeroMitigations() {
        Risk risk = new Risk("t", null, RiskCategory.SECURITY, "o", 4, 5, RiskStatus.OPEN);
        when(repo.findWithMitigationsById(1L)).thenReturn(Optional.of(risk));

        assertThatThrownBy(() -> service.update(1L, request(RiskStatus.CLOSED)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one mitigation");
    }

    @Test
    void canCloseARiskThatHasAMitigation() {
        Risk risk = new Risk("t", null, RiskCategory.SECURITY, "o", 4, 5, RiskStatus.OPEN);
        risk.getMitigations().add(new Mitigation(risk, "MFA", 5));
        when(repo.findWithMitigationsById(1L)).thenReturn(Optional.of(risk));
        when(repo.saveAndFlush(risk)).thenReturn(risk);

        assertThat(service.update(1L, request(RiskStatus.CLOSED)).status()).isEqualTo(RiskStatus.CLOSED);
    }

    @Test
    void nonClosedStatusesAreAlwaysAllowedWithoutMitigations() {
        assertThatCode(() -> {
            RiskService.assertCanBeClosed(RiskStatus.OPEN, 0);
            RiskService.assertCanBeClosed(RiskStatus.MITIGATING, 0);
        }).doesNotThrowAnyException();
    }

    @Test
    void getUnknownRiskIsNotFound() {
        when(repo.findWithMitigationsById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(NotFoundException.class);
    }
}
