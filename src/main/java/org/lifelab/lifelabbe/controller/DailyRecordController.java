package org.lifelab.lifelabbe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.dto.dailyrecord.DailyRecordCreateRequest;
import org.lifelab.lifelabbe.dto.dailyrecord.DailyRecordCreateResponse;
import org.lifelab.lifelabbe.dto.dailyrecord.DailyRecordItemsResponse;
import org.lifelab.lifelabbe.service.DailyRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class DailyRecordController {

    private final DailyRecordService dailyRecordService;

    //일일기록 생성
    @PostMapping("/{experimentId}/daily-record")
    public ResponseEntity<ApiResponse<?>> createDailyRecord(
            Authentication authentication,
            @PathVariable Long experimentId,
            @Valid @RequestBody DailyRecordCreateRequest request
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        DailyRecordCreateResponse response =
                dailyRecordService.create(userId, experimentId, request);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }
    //오늘의 기록 항목 조회
    @GetMapping("/{experimentId}/daily-record/items")
    public ResponseEntity<ApiResponse<?>> getDailyRecordItems(
            Authentication authentication,
            @PathVariable Long experimentId
    ) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());
        DailyRecordItemsResponse response = dailyRecordService.getItems(userId, experimentId);
        return ResponseEntity.ok(ApiResponse.success(200, response));
    }
}
