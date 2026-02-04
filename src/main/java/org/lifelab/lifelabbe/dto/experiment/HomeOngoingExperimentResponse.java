package org.lifelab.lifelabbe.dto.experiment;

public record HomeOngoingExperimentResponse(
        Long experimentId,
        String title,
        Integer dDay,
        String dDayLabel,
        boolean preStateRecorded,
        TodayRecordStatus todayRecordStatus,
        String subtitle
) {
    public static HomeOngoingExperimentResponse of(
            Long experimentId,
            String title,
            int dDay,
            boolean preStateRecorded,
            TodayRecordStatus todayRecordStatus
    ) {
        boolean showDDay = (dDay == 0) || preStateRecorded;

        Integer outDDay = showDDay ? dDay : null;
        String outDDayLabel = showDDay ? ((dDay == 0) ? "D-DAY" : "D-" + dDay) : null;

        String subtitle;
        if (dDay == 0) {
            subtitle = "실험이 완료되었어요! 결과를 확인해보세요";
        } else if (!preStateRecorded) {
            subtitle = "아직 실험 전 상태가 기록되지 않았어요!";
        } else if (todayRecordStatus == TodayRecordStatus.DONE) {
            subtitle = "오늘 기록 완료 ✓";
        } else {
            subtitle = "오늘 기록이 아직 없어요";
        }

        return new HomeOngoingExperimentResponse(
                experimentId,
                title,
                outDDay,
                outDDayLabel,
                preStateRecorded,
                todayRecordStatus,
                subtitle
        );
    }

}
