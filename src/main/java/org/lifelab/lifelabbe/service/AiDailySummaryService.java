package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentPreStateValue;
import org.lifelab.lifelabbe.dto.ai.DailySummaryDtos.AiDailySummaryGenerateRequest;
import org.lifelab.lifelabbe.dto.ai.DailySummaryDtos.AiDailySummaryResponse;
import org.lifelab.lifelabbe.dto.ai.DailySummaryDtos.MetricDeltaDto;
import org.lifelab.lifelabbe.dto.ai.DailySummaryDtos.PreStateDto;
import org.lifelab.lifelabbe.repository.DailyRecordRepository;
import org.lifelab.lifelabbe.repository.DailyRecordValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentPreStateValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.lifelab.lifelabbe.repository.projection.DailyValueRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDailySummaryService {

    private final ExperimentRepository experimentRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final DailyRecordValueRepository dailyRecordValueRepository;
    private final ExperimentPreStateValueRepository preStateValueRepository;
    private final AiDailySummaryAiClient aiClient;

    @Transactional(readOnly = true)
    public AiDailySummaryResponse generate(Long userId, Long experimentId, LocalDate dateOrNull) {

        Experiment exp = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        LocalDate today = (dateOrNull != null)
                ? dateOrNull
                : LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate yesterday = today.minusDays(1);

        //실험 전 상태 값 조회
        List<PreStateDto> preStates = toPreStates(preStateValueRepository.findByExperimentId(experimentId));

        // 오늘 기록 존재 여부
        boolean hasTodayRecord = dailyRecordRepository.existsByExperimentIdAndRecordDate(experimentId, today);

        // 오늘 기록이 없더라도 예외/조기반환 없이 AI 호출
        if (!hasTodayRecord) {
            AiDailySummaryGenerateRequest req = new AiDailySummaryGenerateRequest(
                    safeExperimentName(exp),
                    today.toString(),
                    false,
                    List.of(),
                    preStates
            );

            log.info("[AI DailySummary] hasTodayRecord=false. expId={}, date={}, preStatesCount={}",
                    experimentId, today, preStates.size());

            var aiRes = aiClient.generate(req);

            return new AiDailySummaryResponse(
                    experimentId,
                    today.toString(),
                    aiRes.status(),
                    aiRes.summary()
            );
        }

        // 오늘/어제 값 조회
        List<DailyValueRow> todayRows =
                dailyRecordValueRepository.findDailyValuesByExperimentIdAndRecordDate(experimentId, today);

        List<DailyValueRow> yesterdayRows =
                dailyRecordValueRepository.findDailyValuesByExperimentIdAndRecordDate(experimentId, yesterday);

        Map<String, Float> todayMap = toMap(todayRows);
        Map<String, Float> yesterdayMap = toMap(yesterdayRows);

        //MetricDelta 생성
        List<MetricDeltaDto> metrics = new ArrayList<>();

        for (String key : todayMap.keySet()) {
            if (key == null) continue;

            float t = todayMap.getOrDefault(key, 0f);

            if (!yesterdayMap.containsKey(key)) {
                continue;
            }

            float y = yesterdayMap.get(key);

            metrics.add(new MetricDeltaDto(
                    key,
                    y,
                    t,
                    inferPolarity(key),
                    null
            ));
        }


        AiDailySummaryGenerateRequest req = new AiDailySummaryGenerateRequest(
                safeExperimentName(exp),
                today.toString(),
                true,
                metrics,
                preStates
        );

        log.info("[AI DailySummary] sending. expId={}, date={}, hasTodayRecord=true, metricsCount={}, preStatesCount={}, todayRowsCount={}, yesterdayRowsCount={}",
                experimentId,
                today,
                metrics.size(),
                preStates.size(),
                (todayRows == null ? 0 : todayRows.size()),
                (yesterdayRows == null ? 0 : yesterdayRows.size())
        );

        var aiRes = aiClient.generate(req);

        return new AiDailySummaryResponse(
                experimentId,
                today.toString(),
                aiRes.status(),
                aiRes.summary()
        );
    }

    private Map<String, Float> toMap(List<DailyValueRow> rows) {
        Map<String, Float> map = new LinkedHashMap<>();
        if (rows == null) return map;
        for (DailyValueRow r : rows) {
            if (r == null) continue;
            if (r.getRecordItemKey() == null) continue;
            float v = (float) r.getValue();
            map.put(r.getRecordItemKey(), v);
        }
        return map;
    }

    private List<PreStateDto> toPreStates(List<ExperimentPreStateValue> values) {
        if (values == null || values.isEmpty()) return List.of();

        List<PreStateDto> list = new ArrayList<>();
        for (ExperimentPreStateValue v : values) {
            if (v == null) continue;
            String key = v.getRecordItemKey();
            if (key == null) continue;

            float val;
            try {

                val = ((Number) v.getValue()).floatValue();
            } catch (Exception e) {
                //value가 Number가 아니면 0으로 fallback
                val = 0f;
            }

            list.add(new PreStateDto(
                    key,
                    val,
                    inferPolarity(key),
                    null
            ));
        }
        return list;
    }

    private String inferPolarity(String key) {
        if (key == null) return "HIGHER_IS_BETTER";
        Set<String> lowerIsBetter = Set.of("피로도", "불안", "스트레스", "통증", "졸림");
        if (lowerIsBetter.contains(key)) return "LOWER_IS_BETTER";
        return "HIGHER_IS_BETTER";
    }

    private String safeExperimentName(Experiment exp) {
        String title = exp.getTitle();
        return (title == null || title.isBlank()) ? "해당 실험" : title;
    }
}