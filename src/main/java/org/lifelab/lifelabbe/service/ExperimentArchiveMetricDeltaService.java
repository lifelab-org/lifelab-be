package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentPreStateValue;
import org.lifelab.lifelabbe.dto.archive.ArchiveMetricDeltaResponse;
import org.lifelab.lifelabbe.dto.archive.ChangeDirection;
import org.lifelab.lifelabbe.dto.archive.TopMetricDeltaResponse;
import org.lifelab.lifelabbe.repository.DailyRecordValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentPreStateValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.lifelab.lifelabbe.repository.projection.DailyValueRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExperimentArchiveMetricDeltaService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentPreStateValueRepository preStateValueRepository;
    private final DailyRecordValueRepository dailyRecordValueRepository;

    @Transactional(readOnly = true)
    public ArchiveMetricDeltaResponse getMetricDeltas(Long userId, Long experimentId) {

        Experiment exp = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        List<ExperimentPreStateValue> preValues =
                preStateValueRepository.findByExperimentId(experimentId);

        if (preValues.isEmpty()) {
            throw new GlobalException(ErrorCode.PRE_STATE_404);
        }

        List<ArchiveMetricDeltaResponse.Metric> metrics = new ArrayList<>();

        for (ExperimentPreStateValue pv : preValues) {
            String key = pv.getRecordItemKey();
            double previous = pv.getValue();

            double currentAvg = dailyRecordValueRepository
                    .findAvgValue(
                            experimentId,
                            key,
                            exp.getStartDate(),
                            exp.getEndDate()
                    )
                    .orElseThrow(() -> new GlobalException(ErrorCode.RECORD_404));

            double prev = round(previous, 1);
            double curr = round(currentAvg, 1);

            double diff = curr - prev;

            ChangeDirection direction =
                    diff > 0 ? ChangeDirection.UP :
                            diff < 0 ? ChangeDirection.DOWN :
                                    ChangeDirection.SAME;

            double delta = round(Math.abs(diff), 1);

            metrics.add(new ArchiveMetricDeltaResponse.Metric(
                    key,
                    prev,
                    curr,
                    delta,
                    direction
            ));
        }

        return new ArchiveMetricDeltaResponse(experimentId, metrics);
    }
    @Transactional(readOnly = true)
    public TopMetricDeltaResponse getTopMetricDelta(Long userId, Long experimentId) {

        // 내 실험인지 검증
        experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        // 전상태 값 로드
        List<ExperimentPreStateValue> preValues =
                preStateValueRepository.findByExperimentId(experimentId);

        if (preValues.isEmpty()) {
            throw new GlobalException(ErrorCode.PRE_STATE_404);
        }

        // preMap: key -> preValue
        Map<String, Integer> preMap = new HashMap<>();
        for (ExperimentPreStateValue pv : preValues) {
            preMap.put(pv.getRecordItemKey(), pv.getValue());
        }

        // 기간 전체 기록 row 조회 (날짜 포함)
        List<DailyValueRow> rows =
                dailyRecordValueRepository.findAllDailyValuesByExperimentId(experimentId);

        if (rows.isEmpty()) {
            throw new GlobalException(ErrorCode.RECORD_404);
        }

        // 최대 변화폭 찾기
        String bestKey = null;
        Integer bestPre = null;
        Integer bestCur = null;
        LocalDate bestDate = null;
        int bestAbs = -1;

        for (DailyValueRow r : rows) {
            String key = r.getRecordItemKey();
            Integer curRaw = r.getValue();
            Integer preRaw = preMap.get(key);

            if (preRaw == null || curRaw == null) continue;

            int abs = Math.abs(curRaw - preRaw);

            if (abs > bestAbs) {
                bestAbs = abs;
                bestKey = key;
                bestPre = preRaw;
                bestCur = curRaw;
                bestDate = r.getRecordDate();
                continue;
            }

            // 동점이면 더 최근 날짜 선택
            if (abs == bestAbs && bestDate != null && r.getRecordDate() != null) {
                if (r.getRecordDate().isAfter(bestDate)) {
                    bestKey = key;
                    bestPre = preRaw;
                    bestCur = curRaw;
                    bestDate = r.getRecordDate();
                }
            }
        }

        if (bestKey == null || bestDate == null) {
            throw new GlobalException(ErrorCode.RECORD_404);
        }

        int diff = bestCur - bestPre;

        ChangeDirection direction =
                diff > 0 ? ChangeDirection.UP :
                        diff < 0 ? ChangeDirection.DOWN :
                                ChangeDirection.SAME;

        int delta = Math.abs(diff);

        return new TopMetricDeltaResponse(
                experimentId,
                bestKey,
                bestPre,
                bestCur,
                delta,
                direction,
                bestDate
        );
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }
}