package org.lifelab.lifelabbe.controller;
import org.lifelab.lifelabbe.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.dto.experiment.ExperimentCreateRequest;
import org.lifelab.lifelabbe.dto.experiment.ExperimentCreateResponse;
import org.lifelab.lifelabbe.service.ExperimentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(
            Authentication authentication,
            @Valid @RequestBody ExperimentCreateRequest request
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        experimentService.create(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "실험이 성공적으로 생성되었습니다."));
    }
    // 진행중
    @GetMapping("/ongoing")
    public ResponseEntity<ApiResponse<?>> ongoing(Authentication authentication) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success(200, experimentService.getOngoing(userId)));
    }

    // 진행 전
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<?>> upcoming(Authentication authentication) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success(200, experimentService.getUpcoming(userId)));
    }

    // 결과 확인(클릭) 처리 → 홈2에서 사라지게
    @PostMapping("/{experimentId}/result-check")
    public ResponseEntity<ApiResponse<?>> resultCheck(
            Authentication authentication,
            @PathVariable Long experimentId
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());
        experimentService.markResultChecked(userId, experimentId);
        return ResponseEntity.ok(ApiResponse.success(200, "결과 확인 처리 완료"));
    }

    @GetMapping("/{experimentId}")
    public ResponseEntity<ApiResponse<?>> detail(
            Authentication authentication,
            @PathVariable Long experimentId
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success(200, experimentService.getDetail(userId, experimentId)));
    }

}
