package org.lifelab.lifelabbe.service;

import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.DailyRecord;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.TodayRecordOutcome;
import org.lifelab.lifelabbe.dto.experiment.TodayRecordOutcomeRequest;
import org.lifelab.lifelabbe.dto.experiment.TodayRecordOutcomeResponse;
import org.lifelab.lifelabbe.repository.DailyRecordRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RecordService {

    private final DailyRecordRepository dailyRecordRepository;
    private final ExperimentRepository experimentRepository;

    public RecordService(
            DailyRecordRepository dailyRecordRepository,
            ExperimentRepository experimentRepository
    ) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.experimentRepository = experimentRepository;
    }

    public TodayRecordOutcomeResponse saveTodayRecord(
            Long experimentId,
            TodayRecordOutcomeRequest request
    ) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        LocalDate today = LocalDate.now();

        if (dailyRecordRepository
                .findByExperiment_IdAndRecordDate(experimentId, today)
                .isPresent()) {
            throw new GlobalException(ErrorCode.REC_409);
        }

        TodayRecordOutcome outcome =
                TodayRecordOutcome.valueOf(request.getOutcome().name());

        DailyRecord dailyRecord = new DailyRecord(
                experiment,
                today,
                outcome
        );

        return new TodayRecordOutcomeResponse(
                experiment.getId(),
                request.getOutcome(),
                "OUTCOME_SAVED"
        );

    }
}

