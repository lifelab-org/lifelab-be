package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentStatus;
import org.lifelab.lifelabbe.domain.RecordItem;
import org.lifelab.lifelabbe.dto.experiment.*;
import org.lifelab.lifelabbe.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentService {
    //  사용자당 실험 최대 개수
    private static final int MAX_EXPERIMENT_COUNT = 10;

    //홈/캘린더에서 사용할 실험 색상 팔레트
    private static final List<String> EXPERIMENT_COLOR_PALETTE = List.of(
            "#AFCBEA", // 소프트 스카이블루
            "#F2B885", // 소프트 피치 오렌지
            "#EFD37A", // 버터 옐로우
            "#B8B0AA", // 웜 그레이
            "#CDB7E8", // 라일락
            "#A8D5BA", // 세이지 그린
            "#F3C4D5", // 파스텔 핑크
            "#BFE0E7", // 민트 블루
            "#D9C6A5", // 베이지
            "#C7D9A5", // 라이트 올리브
            "#E6CBA8", // 샌드 베이지
            "#D7C9F2"  // 연보라
    );
    private final ExperimentRepository experimentRepository;
    private final ExperimentPreStateValueRepository preStateValueRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final DailyRecordValueRepository dailyRecordValueRepository;
    private final RecordItemRepository recordItemRepository;


    // 상태 자동 동기화
    @Transactional
    public void syncStatuses(LocalDate today) {
        int completed =
                experimentRepository.updateOngoingToCompleted();

        int ongoing =
                experimentRepository.updateUpcomingToOngoing(today);

        log.info(
                "syncStatuses today={}, ongoing→completed={}, upcoming→ongoing={}",
                today, completed, ongoing
        );
    }
    // 실험 생성 시 저장된 기록 항목 조회
    @Transactional(readOnly = true)
    public ExperimentCreateRecordItemsResponse getCreateRecordItems(Long userId, Long experimentId) {

        // 실험 존재 + 내 실험인지 검증
        Experiment experiment = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        // RecordItem → DTO 변환
        List<ExperimentCreateRecordItemsResponse.RecordItemKeyResponse> values =
                experiment.getRecordItems()
                        .stream()
                        .map(ri ->
                                new ExperimentCreateRecordItemsResponse.RecordItemKeyResponse(
                                        ri.getName()
                                )
                        )
                        .toList();

        // 반환
        return new ExperimentCreateRecordItemsResponse(
                experiment.getId(),
                values
        );
    }
    // 실험 생성
    @Transactional
    public ExperimentCreateResponse create(Long userId, ExperimentCreateRequest req) {

        validateDates(req.startDate(), req.endDate());
        validateRecordItems(req.recordItems());
        // 실험 개수 최대 10개 제한
        validateExperimentCount(userId);
        ExperimentStatus status =
                determineStatus(req.startDate(), req.endDate());
        String color = assignExperimentColor(userId);

        Experiment experiment = Experiment.builder()
                .userId(userId)
                .title(req.title().trim())
                .rule(req.rule().trim())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(status)
                .color(color)
                .build();

        for (var itemReq : req.recordItems()) {
            experiment.addRecordItem(
                    new RecordItem(itemReq.name().trim())
            );
        }

        Experiment saved =
                experimentRepository.save(experiment);

        return new ExperimentCreateResponse(
                saved.getId(),
                "실험이 생성되었습니다."
        );
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
                        .findByUserIdAndStartDateLessThanEqualAndResultCheckedFalseOrderByEndDateAsc(
                                userId, today
                        );

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

                            // 오늘 기록 존재 여부로 DONE/NONE 결정
                            TodayRecordStatus todayRecordStatus =
                                    dailyRecordRepository.existsByExperimentIdAndRecordDate(
                                            e.getId(),
                                            today
                                    )
                                            ? TodayRecordStatus.DONE
                                            : TodayRecordStatus.NONE;

                            return HomeOngoingExperimentResponse.of(
                                    e.getId(),
                                    e.getTitle(),
                                    e.getColor(),
                                    rawDDay,
                                    preStateRecorded,
                                    todayRecordStatus
                            );
                        })
                        .toList();

        return mapped.stream()
                .sorted((a, b) -> {

                    int ap = priority(a);
                    int bp = priority(b);

                    if (ap != bp) return Integer.compare(ap, bp);

                    int ad = a.dDay();
                    int bd = b.dDay();

                    // D-N 그룹 → 남은 것 적은 순
                    if (ap == 3) {
                        return Integer.compare(ad, bd);
                    }

                    // D+N 그룹 → 최근 종료 위
                    if (ap == 4) {
                        return Integer.compare(Math.abs(ad), Math.abs(bd));
                    }

                    return 0;
                })
                .toList();
    }


    // 홈3: 예정 실험
    @Transactional
    public List<HomeUpcomingExperimentResponse> getUpcoming(
            Long userId
    ) {

        LocalDate today = LocalDate.now();

        // status 자동 업데이트
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
                            e.getColor(),
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
                                        new GlobalException(ErrorCode.EXP_404));

        experiment.markResultChecked();
        experiment.changeStatus(ExperimentStatus.COMPLETED);
    }


    // 내부 유틸
    private void validateDates(
            LocalDate start,
            LocalDate end
    ) {
        //시작일이 오늘보다 과거면 생성 불가
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (start.isBefore(today)) {
            throw new GlobalException(ErrorCode.INVALID_START_DATE);
        }
        // 시작일이 종료일보다 늦으면 불가
        if (start.isAfter(end)) {
            throw new GlobalException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateRecordItems(
            List<ExperimentCreateRequest.RecordItemCreate> items
    ) {
        if (items == null || items.isEmpty()) {
            //요청값 비었을 때도 에러코드로
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        Set<String> names = new HashSet<>();

        for (var it : items) {

            String n = it.name().trim();

            if (n.isEmpty())
                throw new GlobalException(ErrorCode.INVALID_PARAMETER);

            if (!names.add(n))
                throw new GlobalException(ErrorCode.DUPLICATE_RECORD_ITEM);
        }

        if (items.size() > 10)
            throw new GlobalException(ErrorCode.TOO_MANY_RECORD_ITEMS);
    }
    // 사용자별 실험 개수 검증
    private void validateExperimentCount(Long userId) {
        long experimentCount = experimentRepository.countByUserId(userId);

        if (experimentCount >= MAX_EXPERIMENT_COUNT) {
            throw new GlobalException(ErrorCode.TOO_MANY_EXPERIMENTS);
        }
    }
    // 사용자별 기존 색상과 겹치지 않게 색상 랜덤 배정
    private String assignExperimentColor(Long userId) {
        Set<String> usedColors =
                new HashSet<>(experimentRepository.findUsedColorsByUserId(userId));

        List<String> availableColors =
                new ArrayList<>(EXPERIMENT_COLOR_PALETTE);

        availableColors.removeAll(usedColors);

        if (availableColors.isEmpty()) {
            throw new GlobalException(ErrorCode.TOO_MANY_EXPERIMENTS);
        }

        Collections.shuffle(availableColors);

        return availableColors.get(0);
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
    @Transactional(readOnly = true)
    public ExperimentDetailResponse getDetail(Long userId, Long experimentId) {

        Experiment e = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        LocalDate today = LocalDate.now();

        int rawDDay = (int) ChronoUnit.DAYS.between(today, e.getEndDate());

        // 오늘 기록 여부
        boolean hasTodayRecord =
                dailyRecordRepository.existsByExperimentIdAndRecordDate(experimentId, today);

        TodayRecordStatus todayRecordStatus =
                hasTodayRecord ? TodayRecordStatus.DONE : TodayRecordStatus.NONE;

        // 기록 항목(실험 생성 시 넣은 RecordItem들)
        List<String> recordItems = e.getRecordItems().stream()
                .map(RecordItem::getName)
                .toList();

        return ExperimentDetailResponse.of(
                e.getId(),
                e.getTitle(),
                e.getStartDate(),
                e.getEndDate(),
                e.totalDaysInclusive(),
                rawDDay,
                e.getRule(),
                todayRecordStatus,
                recordItems
        );
    }
    @Transactional
    public void deleteExperiment(Long userId, Long experimentId) {

        // 내 실험인지 검증
        Experiment experiment = experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        var dailyRecords = dailyRecordRepository.findByExperimentId(experimentId);
        for (var dr : dailyRecords) {
            dailyRecordValueRepository.deleteByDailyRecord_Id(dr.getId());
        }
        dailyRecordRepository.deleteByExperimentId(experimentId);

        //PreState 삭제
        preStateValueRepository.deleteByExperiment_Id(experimentId);

        //RecordItem 삭제
        recordItemRepository.deleteByExperiment_Id(experimentId);

        //Experiment 삭제
        experimentRepository.delete(experiment);
    }
    private int priority(HomeOngoingExperimentResponse r) {

        // 전상태 미기록
        if (!r.preStateRecorded()) return 1;

        int d = r.dDay();

        // 2D-DAY
        if (d == 0) return 2;

        // D-N
        if (d > 0) return 3;

        // D+N
        return 4;
    }


}

