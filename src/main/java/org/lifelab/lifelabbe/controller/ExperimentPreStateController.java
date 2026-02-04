package org.lifelab.lifelabbe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.dto.experiment.PreStateRequest;
import org.lifelab.lifelabbe.service.ExperimentPreStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class ExperimentPreStateController {

    private final ExperimentPreStateService preStateService;

    @PostMapping("/{experimentId}/pre-state")
    public ResponseEntity<ApiResponse<?>> savePreState(
            Authentication authentication,
            @PathVariable Long experimentId,
            @Valid @RequestBody PreStateRequest request
    ) {
        //JWT 필터가 principal에 userId(String)를 넣는 구조
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        preStateService.savePreState(experimentId, userId, request);

        return ResponseEntity
                .ok(ApiResponse.success(200, "실험 전 상태 저장 성공"));
    }
}
