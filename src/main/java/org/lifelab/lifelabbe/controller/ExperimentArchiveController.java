package org.lifelab.lifelabbe.controller;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.service.ExperimentArchiveService;
import org.lifelab.lifelabbe.service.ExperimentArchiveSuccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class ExperimentArchiveController {

    private final ExperimentArchiveService experimentArchiveService;
    private final ExperimentArchiveSuccessService experimentArchiveSuccessService;

    //실험 내역(아카이브) 리스트 조회
    @GetMapping("/archive")
    public ResponseEntity<ApiResponse<?>> archive(Authentication authentication) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        return ResponseEntity.ok(
                ApiResponse.success(200, experimentArchiveService.getArchive(userId))
        );
    }
    // 특정 실험 성공률 조회
    @GetMapping("/{experimentId}/success")
    public ResponseEntity<ApiResponse<?>> successRate(
            Authentication authentication,
            @PathVariable Long experimentId
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        experimentArchiveSuccessService.getSuccessRate(userId, experimentId)
                )
        );
    }
}
