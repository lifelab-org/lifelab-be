package org.lifelab.lifelabbe.controller;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.dto.ai.AiCommentResponse;
import org.lifelab.lifelabbe.service.AiCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class AiCommentController {

    private final AiCommentService aiCommentService;

    @GetMapping("/{experimentId}/ai-comment")
    public ResponseEntity<ApiResponse<?>> get(
            Authentication authentication,
            @PathVariable Long experimentId
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());
        AiCommentResponse response = aiCommentService.getComment(userId, experimentId);
        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

}