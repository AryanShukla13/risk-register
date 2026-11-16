package com.riskregister.risk;

import com.riskregister.risk.dto.RiskRequest;
import com.riskregister.risk.dto.RiskResponse;
import com.riskregister.risk.dto.RiskSummaryResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskService service;

    public RiskController(RiskService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<RiskResponse> create(@Valid @RequestBody RiskRequest req) {
        RiskResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/risks/" + created.id())).body(created);
    }

    @GetMapping
    List<RiskSummaryResponse> list(@RequestParam(required = false) RiskCategory category,
                                   @RequestParam(required = false) RiskStatus status,
                                   @RequestParam(required = false) String sort) {
        return service.list(category, status, RiskSort.parse(sort));
    }

    @GetMapping("/{id}")
    RiskResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    RiskResponse update(@PathVariable long id, @Valid @RequestBody RiskRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
