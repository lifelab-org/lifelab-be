package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.dto.archive.ArchiveGraphResponse;
import org.lifelab.lifelabbe.repository.DailyRecordValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExperimentArchiveGraphService {

    private final ExperimentRepository experimentRepository;
    private final DailyRecordValueRepository dailyRecordValueRepository;

    //그래프용 지표별 변화 데이터 조회 (기록된 날짜만 내려줌)
    @Transactional(readOnly = true)
    public ArchiveGraphResponse getGraph(Long userId, Long experimentId, LocalDate from, LocalDate to) {

        Experiment exp = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        // from/to 유효성
        if (from == null) from = exp.getStartDate();
        if (to == null) to = exp.getEndDate();
        if (from.isAfter(to)) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        // 기록된 row만 조회
        List<Object[]> rows = dailyRecordValueRepository.findGraphRowsRaw(experimentId, from, to);

        // 지표명별로 points 누적
        Map<String, List<ArchiveGraphResponse.Point>> metricPointsMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String recordItemKey = (String) row[0];
            LocalDate recordDate = (LocalDate) row[1];
            Object v = row[2];

            if (recordItemKey == null || recordDate == null) continue;

            Integer value = convertToInteger(v);

            metricPointsMap
                    .computeIfAbsent(recordItemKey, k -> new ArrayList<>())
                    .add(ArchiveGraphResponse.Point.builder()
                            .date(recordDate)
                            .value(value)
                            .build());
        }

        List<ArchiveGraphResponse.MetricSeries> metrics = new ArrayList<>();
        for (var entry : metricPointsMap.entrySet()) {
            metrics.add(ArchiveGraphResponse.MetricSeries.builder()
                    .name(entry.getKey())
                    .points(entry.getValue())
                    .build());
        }

        return ArchiveGraphResponse.builder()
                .experimentId(exp.getId())
                .title(exp.getTitle())
                .startDate(exp.getStartDate())
                .endDate(exp.getEndDate())
                .metrics(metrics)
                .build();
    }

    // value 타입이 Integer가 아닐 수도 있어서 방어 변환
    private Integer convertToInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l.intValue();
        if (v instanceof BigDecimal bd) return bd.intValue();
        if (v instanceof Double d) return (int) Math.round(d); //avg 같은 값 섞였을 때 방어
        return null;
    }
}