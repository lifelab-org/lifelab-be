package org.lifelab.lifelabbe.dto.experiment;

import java.time.LocalDate;
import java.util.List;

public record ExperimentDetailResponse(
        Long experimentId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int dDay,
        String dDayLabel,
        String rule,

        TodayRecordStatus todayRecordStatus,
        List<String> recordItems
) {
    public static ExperimentDetailResponse of(
            Long experimentId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            int totalDays,
            int rawDDay,
            String rule,
            TodayRecordStatus todayRecordStatus,
            List<String> recordItems
    ) {
        return new ExperimentDetailResponse(
                experimentId,
                title,
                startDate,
                endDate,
                totalDays,
                rawDDay,
                makeLabel(rawDDay),
                rule,
                todayRecordStatus,
                recordItems
        );
    }

    private static String makeLabel(int rawDDay) {
        if (rawDDay == 0) return "D-DAY";
        if (rawDDay > 0) return "D-" + rawDDay;
        return "D+" + (-rawDDay);
    }
}
