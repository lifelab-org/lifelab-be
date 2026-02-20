package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentPreStateValue;
import org.lifelab.lifelabbe.dto.archive.ArchiveMetricDeltaResponse;
import org.lifelab.lifelabbe.dto.archive.ChangeDirection;
import org.lifelab.lifelabbe.repository.DailyRecordValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentPreStateValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }
}