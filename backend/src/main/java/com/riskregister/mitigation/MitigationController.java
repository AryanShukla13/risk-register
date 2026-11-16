package com.riskregister.mitigation;

import com.riskregister.mitigation.dto.MitigationRequest;
import com.riskregister.mitigation.dto.MitigationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risks/{riskId}/mitigations")
public class MitigationController {

    private final MitigationService service;

    public MitigationController(MitigationService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<MitigationResponse> create(@PathVariable long riskId, @Valid @RequestBody MitigationRequest req) {
        MitigationResponse created = service.create(riskId, req);
        return ResponseEntity.created(URI.create("/api/risks/" + riskId + "/mitigations/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    MitigationResponse update(@PathVariable long riskId, @PathVariable long id,
                              @Valid @RequestBody MitigationRequest req) {
        return service.update(riskId, id, req);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable long riskId, @PathVariable long id) {
        service.delete(riskId, id);
        return ResponseEntity.noContent().build();
    }
}
