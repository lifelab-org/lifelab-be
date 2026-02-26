package org.lifelab.lifelabbe.controller;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ApiResponse;
import org.lifelab.lifelabbe.dto.experiment.CalendarMonthResponse;
import org.lifelab.lifelabbe.service.CalendarService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/experiments")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/calendar")
    public ApiResponse<CalendarMonthResponse> getCalendarByMonth(
            @RequestParam String month
    ) {


        Long userId = 1L; //이거 임시용 나중에 밑 userId 풀어야됨

//        Long userId = Long.parseLong(
//                SecurityContextHolder.getContext()
//                        .getAuthentication()
//                        .getName()
//        );

        CalendarMonthResponse response =
                calendarService.getExperimentsByMonth(month, userId);

        return ApiResponse.success(200, response);
    }
}