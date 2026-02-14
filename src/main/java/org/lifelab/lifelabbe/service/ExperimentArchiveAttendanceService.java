package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.dto.archive.ArchiveAttendanceRateResponse;
import org.lifelab.lifelabbe.repository.DailyRecordRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ExperimentArchiveAttendanceService {

    private final ExperimentRepository experimentRepository;
    private final DailyRecordRepository dailyRecordRepository;

    @Transactional(readOnly = true)
    public ArchiveAttendanceRateResponse getAttendanceRate(Long userId, Long experimentId) {

        Experiment exp = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        // 전체 실험 일수 (inclusive)
        long totalDays =
                ChronoUnit.DAYS.between(exp.getStartDate(), exp.getEndDate()) + 1;

        if (totalDays <= 0) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        // 기록한 일수
        long recordedDays =
                dailyRecordRepository.countByExperimentId(experimentId);

        int attendanceRate =
                (int) ((recordedDays * 100) / totalDays);

        return new ArchiveAttendanceRateResponse(
                String.valueOf(experimentId),
                attendanceRate
        );
    }
}
