package org.lifelab.lifelabbe.dto.experiment;

import java.time.LocalDate;

public record ExperimentDetailResponse(
        Long experimentId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int dDay,            // raw dDay (0, 양수, 음수)
        String dDayLabel,    // D-8 / D-DAY / D+1
        String rule
) {
    public static ExperimentDetailResponse of(
            Long experimentId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            int totalDays,
            int rawDDay,
            String rule
    ) {
        return new ExperimentDetailResponse(
                experimentId,
                title,
                startDate,
                endDate,
                totalDays,
                rawDDay,
                makeLabel(rawDDay),
                rule
        );
    }

    private static String makeLabel(int rawDDay) {
        if (rawDDay == 0) return "D-DAY";
        if (rawDDay > 0) return "D-" + rawDDay;
        return "D+" + (-rawDDay);
    }
}
