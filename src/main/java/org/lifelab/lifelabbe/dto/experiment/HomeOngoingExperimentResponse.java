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
        boolean isAfterEnd = rawDDay < 0; // D+N (종료 후)
        boolean isDDay = rawDDay == 0;    // D-DAY

        boolean showDDay = isAfterEnd || isDDay || preStateRecorded;

        Integer outDDay = rawDDay;
        String outDDayLabel = showDDay ? makeLabel(rawDDay) : null;

        String subtitle;

        // D+N(종료 후)이면 무조건 결과 확인
        if (isAfterEnd) {
            subtitle = "실험이 완료되었어요! 결과를 확인해보세요";
        }
        // D+N 아니면: 전상태 먼저
        else if (!preStateRecorded) {
            subtitle = "아직 실험 전 상태가 기록되지 않았어요!";
        }
        // 전상태 했으면: 오늘 기록 여부
        else if (todayRecordStatus != TodayRecordStatus.DONE) {
            subtitle = "오늘 기록이 아직 없어요";
        }
        // 오늘 기록 DONE이면: D-DAY일 때만 결과 확인 유도
        else if (isDDay) {
            subtitle = "실험이 완료되었어요! 결과를 확인해보세요";
        }
        // 그 외(일반 진행 중 + 오늘 기록 완료)
        else {
            subtitle = "오늘 기록 완료 ✓";
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
