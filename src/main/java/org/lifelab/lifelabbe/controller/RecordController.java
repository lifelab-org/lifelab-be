package org.lifelab.lifelabbe.controller;

import jakarta.validation.Valid;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.dto.experiment.TodayRecordOutcomeRequest;
import org.lifelab.lifelabbe.dto.experiment.TodayRecordOutcomeResponse;
import org.lifelab.lifelabbe.service.RecordService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experiments")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping("/{experimentId}/records")
    public ApiResponse<TodayRecordOutcomeResponse> saveTodayRecord(
            @PathVariable Long experimentId,
            @RequestBody @Valid TodayRecordOutcomeRequest request
    ) {
        TodayRecordOutcomeResponse response =
                recordService.saveTodayRecord(experimentId, request);

        return ApiResponse.success(200, response);
    }
}
