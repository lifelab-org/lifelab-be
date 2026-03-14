package org.lifelab.lifelabbe.controller;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.dto.archive.ArchiveGraphResponse;
import org.lifelab.lifelabbe.service.ExperimentArchiveGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class ExperimentArchiveGraphController {

    private final ExperimentArchiveGraphService graphService;

    //그래프(라인차트)용 지표별 날짜-값 리스트 조회
    @GetMapping("/{experimentId}/archive/metrics/graph")
    public ResponseEntity<ApiResponse<?>> getGraph(
            Authentication authentication,
            @PathVariable Long experimentId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        ArchiveGraphResponse response =
                graphService.getGraph(userId, experimentId, from, to);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }
}