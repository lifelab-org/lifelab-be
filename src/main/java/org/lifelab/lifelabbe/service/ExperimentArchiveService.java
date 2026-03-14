package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentStatus;
import org.lifelab.lifelabbe.dto.archive.ArchiveListResponse;
import org.lifelab.lifelabbe.repository.DailyRecordRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentArchiveService {

    private final ExperimentRepository experimentRepository;
    private final DailyRecordRepository dailyRecordRepository;

    @Transactional
    public ArchiveListResponse getArchive(Long userId) {

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        experimentRepository.updateUpcomingToOngoing(today);
        experimentRepository.updateOngoingToCompleted();

        List<Experiment> completed =
                experimentRepository.findByUserIdAndStatusOrderByEndDateDesc(userId, ExperimentStatus.COMPLETED);

        if (completed.isEmpty()) {
            throw new GlobalException(ErrorCode.ARCHIVE_404);
        }

        List<ArchiveListResponse.ArchiveExperimentDto> experiments = completed.stream()
                .map(e -> {
                    // 기록이 하나라도 있으면 true(체크), 없으면 false(엑스)
                    boolean isSuccess = dailyRecordRepository.existsByExperimentId(e.getId());

                    return new ArchiveListResponse.ArchiveExperimentDto(
                            String.valueOf(e.getId()),
                            e.getTitle(),
                            e.getStartDate(),
                            e.getEndDate(),
                            isSuccess
                    );
                })
                .toList();

        return new ArchiveListResponse(experiments);
    }
}
