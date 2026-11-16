package com.riskregister.mitigation;

import com.riskregister.common.BusinessRuleException;
import com.riskregister.common.NotFoundException;
import com.riskregister.mitigation.dto.MitigationRequest;
import com.riskregister.mitigation.dto.MitigationResponse;
import com.riskregister.risk.Risk;
import com.riskregister.risk.RiskRepository;
import com.riskregister.risk.RiskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MitigationService {

    static final String DELETE_LAST_ON_CLOSED =
            "The last mitigation of a closed risk cannot be deleted. Reopen the risk first.";

    private final MitigationRepository mitigations;
    private final RiskRepository risks;

    public MitigationService(MitigationRepository mitigations, RiskRepository risks) {
        this.mitigations = mitigations;
        this.risks = risks;
    }

    public MitigationResponse create(long riskId, MitigationRequest req) {
        Risk risk = findRisk(riskId);
        Mitigation saved = mitigations.save(new Mitigation(risk, req.description().strip(), req.effectiveness()));
        return MitigationMapper.toResponse(saved);
    }

    public MitigationResponse update(long riskId, long id, MitigationRequest req) {
        Mitigation m = find(riskId, id);
        m.apply(req.description().strip(), req.effectiveness());
        return MitigationMapper.toResponse(m);
    }

    public void delete(long riskId, long id) {
        Mitigation m = find(riskId, id);
        if (m.getRisk().getStatus() == RiskStatus.CLOSED && mitigations.countByRiskId(riskId) == 1) {
            throw new BusinessRuleException(DELETE_LAST_ON_CLOSED);
        }
        mitigations.delete(m);
    }

    private Risk findRisk(long riskId) {
        return risks.findById(riskId).orElseThrow(() -> new NotFoundException("Risk " + riskId + " not found"));
    }

    private Mitigation find(long riskId, long id) {
        return mitigations.findByIdAndRiskId(id, riskId)
                .orElseThrow(() -> new NotFoundException(
                        "Mitigation " + id + " not found on risk " + riskId));
    }
}
