package org.lifelab.lifelabbe.controller;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.dto.archive.ArchiveMetricDeltaResponse;
import org.lifelab.lifelabbe.service.ExperimentArchiveMetricDeltaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class ExperimentArchiveMetricDeltaController {

    private final ExperimentArchiveMetricDeltaService metricDeltaService;

    @GetMapping("/{experimentId}/archive/metrics")
    public ResponseEntity<ApiResponse<?>> getMetricDeltas(
            Authentication authentication,
            @PathVariable Long experimentId
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        ArchiveMetricDeltaResponse res =
                metricDeltaService.getMetricDeltas(userId, experimentId);

        return ResponseEntity.ok(ApiResponse.success(200, res));
    }
}