package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentPreStateValue;
import org.lifelab.lifelabbe.dto.ai.AiCommentGenerateRequest;
import org.lifelab.lifelabbe.dto.ai.AiCommentResponse;
import org.lifelab.lifelabbe.dto.ai.MetricChangeDto;
import org.lifelab.lifelabbe.repository.DailyRecordRepository;
import org.lifelab.lifelabbe.repository.DailyRecordValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentPreStateValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCommentService {

    private static final int MIN_DAILY_RECORDS_TO_GENERATE = 3;

    private final ExperimentRepository experimentRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final DailyRecordValueRepository dailyRecordValueRepository;
    private final ExperimentPreStateValueRepository preStateValueRepository;
    private final AiCommentAiClient aiClient;

    @Transactional(readOnly = true)
    public AiCommentResponse getComment(Long userId, Long experimentId) {
        return generate(userId, experimentId);
    }

    //매번 FastAPI 호출해서 즉시 생성(저장 없음)
    @Transactional(readOnly = true)
    public AiCommentResponse generateComment(Long userId, Long experimentId) {
        return generate(userId, experimentId);
    }

    @Transactional(readOnly = true)
    public AiCommentResponse generate(Long userId, Long experimentId) {

        Experiment exp = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        long dailyCount = dailyRecordRepository.countByExperimentId(experimentId);
        if (dailyCount < MIN_DAILY_RECORDS_TO_GENERATE) {
            throw new GlobalException(ErrorCode.AI_400);
        }

        // 실험 기간
        LocalDate startDate = exp.getStartDate();
        LocalDate endDate = exp.getEndDate();
        if (startDate == null || endDate == null) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        // 출석률 = 기록일수 / 전체기간일수 * 100
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (totalDays <= 0) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }
        float attendanceRate = (float) dailyCount * 100.0f / (float) totalDays;

        String startDateStr = startDate.toString();
        String endDateStr = endDate.toString();

        //pre 목록
        List<ExperimentPreStateValue> pres = preStateValueRepository.findByExperimentId(experimentId);
        if (pres.isEmpty()) {
            throw new GlobalException(ErrorCode.PRE_STATE_404);
        }

        //TopMetric 계산
        TopMetricResult top = pres.stream()
                .map(p -> {
                    String key = p.getRecordItemKey();
                    double before = p.getValue();
                    double after = dailyRecordValueRepository
                            .findAvgValue(experimentId, key, startDate, endDate)
                            .orElse(before);
                    double absDelta = Math.abs(after - before);
                    return new TopMetricResult(key, before, after, absDelta);
                })
                .max(Comparator.comparingDouble(TopMetricResult::absDelta))
                .orElseThrow(() -> new GlobalException(ErrorCode.AI_400));

        //FastAPI 요청 DTO
        String experimentName = safeExperimentName(exp);

        // metrics 구성 (polarity 필수 → 임시 규칙 적용)
        List<MetricChangeDto> metrics = pres.stream()
                .map(p -> {
                    String key = p.getRecordItemKey();
                    float before = (float) p.getValue();
                    float after = dailyRecordValueRepository
                            .findAvgValue(experimentId, key, startDate, endDate)
                            .map(Double::floatValue)
                            .orElse(before);

                    return new MetricChangeDto(
                            key,
                            before,
                            after,
                            inferPolarity(key),
                            null
                    );
                })
                .toList();

        String topChangedMetricKey = top.key();
        float safeAttendanceRate = Math.max(0f, attendanceRate);

        AiCommentGenerateRequest req = new AiCommentGenerateRequest(
                experimentName,
                startDateStr,
                endDateStr,
                safeAttendanceRate,
                null, // successRate
                metrics,
                topChangedMetricKey
        );

        // 저장 없이 호출 결과만 반환
        String generatedText = aiClient.generateComment(req);

        return new AiCommentResponse(experimentId, generatedText, true);
    }

    private String inferPolarity(String key) {
        if (key == null) return "HIGHER_IS_BETTER";
        // 최소 동작용 임시 규칙
        var lowerIsBetter = java.util.Set.of("피로도", "불안", "스트레스", "통증", "졸림");
        if (lowerIsBetter.contains(key)) return "LOWER_IS_BETTER";
        return "HIGHER_IS_BETTER";
    }

    private String safeExperimentName(Experiment exp) {
        String title = exp.getTitle();
        return (title == null || title.isBlank()) ? "해당" : title;
    }

    private record TopMetricResult(String key, double before, double after, double absDelta) {}
}