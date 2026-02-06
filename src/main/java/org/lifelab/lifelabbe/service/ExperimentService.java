package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentStatus;
import org.lifelab.lifelabbe.domain.RecordItem;
import org.lifelab.lifelabbe.dto.experiment.*;
import org.lifelab.lifelabbe.repository.ExperimentPreStateValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentPreStateValueRepository preStateValueRepository;

    // 상태 자동 동기화
    @Transactional
    public void syncStatuses(LocalDate today) {
        int completed =
                experimentRepository.updateOngoingToCompleted(today);

        int ongoing =
                experimentRepository.updateUpcomingToOngoing(today);

        log.info(
                "syncStatuses today={}, ongoing→completed={}, upcoming→ongoing={}",
                today, completed, ongoing
        );
    }

    // 실험 생성
    @Transactional
    public ExperimentCreateResponse create(Long userId, ExperimentCreateRequest req) {

        validateDates(req.startDate(), req.endDate());
        validateRecordItems(req.recordItems());

        ExperimentStatus status =
                determineStatus(req.startDate(), req.endDate());

        Experiment experiment = Experiment.builder()
                .userId(userId)
                .title(req.title().trim())
                .rule(req.rule().trim())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(status)
                .build();

        for (var itemReq : req.recordItems()) {
            experiment.addRecordItem(
                    new RecordItem(itemReq.name().trim())
            );
        }

        Experiment saved =
                experimentRepository.save(experiment);

        return ExperimentCreateResponse.builder()
                .experimentId(saved.getId())
                .title(saved.getTitle())
                .status(status.name())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .totalDays(saved.totalDaysInclusive())
                .recordItems(
                        saved.getRecordItems().stream()
                                .map(ri ->
                                        ExperimentCreateResponse
                                                .RecordItemDto.builder()
                                                .recordItemId(ri.getId())
                                                .name(ri.getName())
                                                .build()
                                )
                                .collect(Collectors.toList())
                )
                .build();
    }

    // 홈2: 진행중 실험
    @Transactional
    public List<HomeOngoingExperimentResponse> getOngoing(Long userId) {

        LocalDate today = LocalDate.now();

        // status 자동 업데이트
        syncStatuses(today);

        log.info("getOngoing userId={}, today={}", userId, today);

        List<Experiment> experiments =
                experimentRepository
                        .findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndResultCheckedFalseOrderByEndDateAsc(
                                userId, today, today
                        );

        TodayRecordStatus todayRecordStatus =
                TodayRecordStatus.NOT_AVAILABLE;

        List<HomeOngoingExperimentResponse> mapped =
                experiments.stream()
                        .map(e -> {

                            int rawDDay =
                                    (int) ChronoUnit.DAYS.between(
                                            today,
                                            e.getEndDate()
                                    );

                            boolean preStateRecorded =
                                    preStateValueRepository
                                            .existsByExperiment_Id(
                                                    e.getId()
                                            );

                            return HomeOngoingExperimentResponse.of(
                                    e.getId(),
                                    e.getTitle(),
                                    rawDDay,
                                    preStateRecorded,
                                    todayRecordStatus
                            );
                        })
                        .toList();

        return mapped.stream()
                .sorted(
                        java.util.Comparator
                                .comparingInt(
                                        (HomeOngoingExperimentResponse r) ->
                                                (r.dDay() != null
                                                        && r.dDay() == 0)
                                                        ? 0
                                                        : 1
                                )
                                .thenComparingInt(
                                        r ->
                                                r.dDay() == null
                                                        ? 1
                                                        : 0
                                )
                                .thenComparingInt(
                                        r ->
                                                r.dDay() == null
                                                        ? Integer.MAX_VALUE
                                                        : Math.abs(r.dDay())
                                )
                )
                .toList();
    }

    // 홈3: 예정 실험
    @Transactional
    public List<HomeUpcomingExperimentResponse> getUpcoming(
            Long userId
    ) {

        LocalDate today = LocalDate.now();

        // ⭐ status 자동 업데이트
        syncStatuses(today);

        log.info("getUpcoming userId={}, today={}", userId, today);

        return experimentRepository
                .findByUserIdAndStartDateAfterOrderByStartDateAsc(
                        userId, today
                )
                .stream()
                .map(e -> {

                    int dDay =
                            (int) ChronoUnit.DAYS.between(
                                    today,
                                    e.getStartDate()
                            );

                    return HomeUpcomingExperimentResponse.of(
                            e.getId(),
                            e.getTitle(),
                            dDay
                    );
                })
                .toList();
    }

    //결과 확인 체크
    @Transactional
    public void markResultChecked(
            Long userId,
            Long experimentId
    ) {

        Experiment experiment =
                experimentRepository
                        .findByIdAndUserId(
                                experimentId,
                                userId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "해당 실험을 찾을 수 없습니다."
                                        )
                        );

        experiment.markResultChecked();
    }

    // 내부 유틸
    private void validateDates(
            LocalDate start,
            LocalDate end
    ) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "startDate는 endDate보다 늦을 수 없습니다."
            );
        }
    }

    private void validateRecordItems(
            List<ExperimentCreateRequest.RecordItemCreate> items
    ) {

        Set<String> names = new HashSet<>();

        for (var it : items) {

            String n = it.name().trim();

            if (n.isEmpty())
                throw new IllegalArgumentException(
                        "recordItems.name은 비어있을 수 없습니다."
                );

            if (!names.add(n))
                throw new IllegalArgumentException(
                        "recordItems.name이 중복되었습니다: " + n
                );
        }

        if (items.size() > 10)
            throw new IllegalArgumentException(
                    "recordItems는 최대 10개까지 가능합니다."
            );
    }

    private ExperimentStatus determineStatus(
            LocalDate start,
            LocalDate end
    ) {

        LocalDate today = LocalDate.now();

        if (today.isBefore(start))
            return ExperimentStatus.UPCOMING;

        if (today.isAfter(end))
            return ExperimentStatus.COMPLETED;

        return ExperimentStatus.ONGOING;
    }
}
