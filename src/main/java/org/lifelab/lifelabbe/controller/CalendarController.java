package org.lifelab.lifelabbe.controller;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.dto.experiment.CalendarMonthResponse;
import org.lifelab.lifelabbe.service.CalendarService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/calendar")
    public ApiResponse<CalendarMonthResponse> getCalendarByMonth(
            Authentication authentication,
            @RequestParam String month
    ) {
        // JWT 필터가 principal에 userId(String)를 넣는 구조
        Long userId = Long.valueOf((String) authentication.getPrincipal());

        CalendarMonthResponse response =
                calendarService.getExperimentsByMonth(month, userId);

        return ApiResponse.success(200, response);
    }
}