package org.lifelab.lifelabbe.controller;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.service.AiDailySummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class AiDailySummaryController {

    private final AiDailySummaryService aiDailySummaryService;

     //한줄요약
    @GetMapping("/{experimentId}/daily-summary")
    public ResponseEntity<ApiResponse<?>> dailySummary(
            Authentication authentication,
            @PathVariable Long experimentId,
            @RequestParam(required = false) String date
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        LocalDate parsedDate = null;
        if (date != null && !date.isBlank()) {
            parsedDate = LocalDate.parse(date); // "YYYY-MM-DD" 형식
        }

        return ResponseEntity.ok(
                ApiResponse.success(200, aiDailySummaryService.generate(userId, experimentId, parsedDate))
        );
    }
}