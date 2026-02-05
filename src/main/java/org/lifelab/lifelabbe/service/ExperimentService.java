package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentPreStateValueRepository preStateValueRepository;

    @Transactional
    public ExperimentCreateResponse create(Long userId, ExperimentCreateRequest req) {
        validateDates(req.startDate(), req.endDate());
        validateRecordItems(req.recordItems());

        // 오늘 기준으로 status 결정
        ExperimentStatus status = determineStatus(req.startDate(), req.endDate());

        Experiment experiment = Experiment.builder()
                .userId(userId)
                .title(req.title().trim())
                .rule(req.rule().trim())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(status) // 고정 ONGOING 제거
                .build();

        for (var itemReq : req.recordItems()) {
            experiment.addRecordItem(new RecordItem(itemReq.name().trim()));
        }

        Experiment saved = experimentRepository.save(experiment);

        return ExperimentCreateResponse.builder()
                .experimentId(saved.getId())
                .title(saved.getTitle())
                .status(status.name())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .totalDays(saved.totalDaysInclusive())
                .recordItems(saved.getRecordItems().stream()
                        .map(ri -> ExperimentCreateResponse.RecordItemDto.builder()
                                .recordItemId(ri.getId())
                                .name(ri.getName())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("startDate는 endDate보다 늦을 수 없습니다.");
        }
    }

    private void validateRecordItems(List<ExperimentCreateRequest.RecordItemCreate> items) {
        Set<String> names = new HashSet<>();
        for (var it : items) {
            String n = it.name().trim();
            if (n.isEmpty()) throw new IllegalArgumentException("recordItems.name은 비어있을 수 없습니다.");
            if (!names.add(n)) throw new IllegalArgumentException("recordItems.name이 중복되었습니다: " + n);
        }

        if (items.size() > 10) throw new IllegalArgumentException("recordItems는 최대 10개까지 가능합니다.");
    }

    private ExperimentStatus determineStatus(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();

        if (today.isBefore(start)) return ExperimentStatus.UPCOMING;
        if (today.isAfter(end)) return ExperimentStatus.COMPLETED;
        return ExperimentStatus.ONGOING;
    }

    @Transactional(readOnly = true)
    public List<HomeOngoingExperimentResponse> getOngoing(Long userId) {
        LocalDate today = LocalDate.now();

        List<Experiment> experiments =
                experimentRepository.findByUserIdAndStatusAndResultCheckedFalseOrderByEndDateAsc(
                        userId, ExperimentStatus.ONGOING
                );

        // (일일기록 아직 없으니 임시)
        TodayRecordStatus todayRecordStatus = TodayRecordStatus.NOT_AVAILABLE;

        List<HomeOngoingExperimentResponse> mapped = experiments.stream()
                .map(e -> {
                    int rawDDay = (int) ChronoUnit.DAYS.between(today, e.getEndDate()); // ✅ 음수면 D+N
                    boolean preStateRecorded = preStateValueRepository.existsByExperiment_Id(e.getId());

                    return HomeOngoingExperimentResponse.of(
                            e.getId(),
                            e.getTitle(),
                            rawDDay,
                            preStateRecorded,
                            todayRecordStatus
                    );
                })
                .toList();

        // ✅ 정렬 규칙:
        // 1) D-DAY(0) 최상단
        // 2) 그 다음 D-(양수) 작은 순
        // 3) 마지막: preStateRecorded=false 이면서 (rawDDay>0)인 애들 중 dDay null인 애들 맨 아래
        //    (HomeOngoingExperimentResponse에서 dDay가 null이면 실험전 미기록 + 아직 완료 전 상태)
        return mapped.stream()
                .sorted(
                        java.util.Comparator
                                .comparingInt((HomeOngoingExperimentResponse r) ->
                                        (r.dDay() != null && r.dDay() == 0) ? 0 : 1
                                )
                                .thenComparingInt(r -> r.dDay() == null ? 1 : 0)
                                .thenComparingInt(r -> r.dDay() == null ? Integer.MAX_VALUE : Math.abs(r.dDay()))
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeUpcomingExperimentResponse> getUpcoming(Long userId) {
        LocalDate today = LocalDate.now();

        return experimentRepository
                .findByUserIdAndStatusOrderByStartDateAsc(userId, ExperimentStatus.UPCOMING)
                .stream()
                .map(e -> {
                    int dDay = (int) ChronoUnit.DAYS.between(today, e.getStartDate());
                    return HomeUpcomingExperimentResponse.of(e.getId(), e.getTitle(), dDay);
                })
                .toList();
    }
    @Transactional
    public void markResultChecked(Long userId, Long experimentId) {
        Experiment experiment = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 실험을 찾을 수 없습니다."));

        experiment.markResultChecked();
        experiment.markCompleted(); // 원하면 유지, 아니면 삭제 가능

        // JPA 더티체킹으로 저장됨 (save 호출 없어도 됨)
    }



}
