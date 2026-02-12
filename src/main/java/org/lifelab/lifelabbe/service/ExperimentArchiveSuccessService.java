package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.DailyRecord.DailyOutcome;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.dto.archive.ArchiveSuccessRateResponse;
import org.lifelab.lifelabbe.repository.DailyRecordRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentArchiveSuccessService {

    private final ExperimentRepository experimentRepository;
    private final DailyRecordRepository dailyRecordRepository;

    @Transactional(readOnly = true)
    public ArchiveSuccessRateResponse getSuccessRate(Long userId, Long experimentId) {

        Experiment exp = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        long successDays = dailyRecordRepository.countByExperiment_IdAndOutcome(experimentId, DailyOutcome.SUCCESS);
        long failDays    = dailyRecordRepository.countByExperiment_IdAndOutcome(experimentId, DailyOutcome.FAIL);
        long total = successDays + failDays;

        int successRate = (total == 0)
                ? 0
                : (int) Math.round((successDays * 100.0) / total);

        //기록이 하나라도 있으면 true, 없으면 false
        boolean isSuccess =
                dailyRecordRepository.existsByExperimentId(experimentId);

        ArchiveSuccessRateResponse.Item item =
                new ArchiveSuccessRateResponse.Item(
                        String.valueOf(exp.getId()),
                        exp.getTitle(),
                        exp.getStartDate(),
                        exp.getEndDate(),
                        isSuccess,
                        successRate
                );

        return new ArchiveSuccessRateResponse(List.of(item));
    }
}
