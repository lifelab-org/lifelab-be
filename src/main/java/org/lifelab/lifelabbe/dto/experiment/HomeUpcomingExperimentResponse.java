package org.lifelab.lifelabbe.dto.experiment;

public record HomeUpcomingExperimentResponse(
        Long experimentId,
        String title,
        String color,
        int dDay,          // 시작일까지 남은 날짜
        String dDayLabel   // "D-4"
) {
    public static HomeUpcomingExperimentResponse of(Long id, String title, String color,int dDay) {
        return new HomeUpcomingExperimentResponse(
                id,
                title,
                color,
                dDay,
                "D-" + dDay
        );
    }
}
