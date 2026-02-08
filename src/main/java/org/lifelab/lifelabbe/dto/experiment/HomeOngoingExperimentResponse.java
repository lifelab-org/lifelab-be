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
            int rawDDay,
            boolean preStateRecorded,
            TodayRecordStatus todayRecordStatus
    ) {
        boolean isFinishedOrDDay = rawDDay <= 0;
        boolean showDDay = isFinishedOrDDay || preStateRecorded;

        Integer outDDay = rawDDay; //  null 금지
        String outDDayLabel = showDDay ? makeLabel(rawDDay) : null;

        String subtitle;
        if (isFinishedOrDDay) {
            // 기간 끝났으면 무조건 결과 확인 유도
            subtitle = "실험이 완료되었어요! 결과를 확인해보세요";
        } else if (!preStateRecorded) {
            subtitle = "아직 실험 전 상태가 기록되지 않았어요!";
        } else if (todayRecordStatus == TodayRecordStatus.DONE) {
            subtitle = "오늘 기록 완료 ✓";
        } else {
            // (NONE 또는 NOT_AVAILABLE 모두 여기로)
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

    private static String makeLabel(int rawDDay) {
        if (rawDDay == 0) return "D-DAY";
        if (rawDDay > 0) return "D-" + rawDDay;
        return "D+" + (-rawDDay); // endDate 지났는데 확인 안 하면 D+N
    }
}
