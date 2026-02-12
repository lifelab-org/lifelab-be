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
    private final DailyRecordRepository dailyRecordRepository;
    private final DailyRecordValueRepository dailyRecordValueRepository;
    private final RecordItemRepository recordItemRepository;


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
                                    rawDDay,
                                    preStateRecorded,
                                    todayRecordStatus
                            );
                        })
                        .toList();

        //D-DAY(0) 최상단
        //dDay null(=preState 미기록 & D-DAY 아님) 최하단  ← (현재는 dDay null이 안 나옴: outDDay=rawDDay라서)
        //나머지는 "급한 순": 이미 지난 것(D+N) 먼저, 그리고 남은 것(D-N) 적은 순
        return mapped.stream()
                .sorted((a, b) -> {
                    boolean aNull = (a.dDay() == null);
                    boolean bNull = (b.dDay() == null);
                    if (aNull != bNull) return aNull ? 1 : -1;

                    int ad = a.dDay();
                    int bd = b.dDay();

                    boolean aDDay = (ad == 0);
                    boolean bDDay = (bd == 0);
                    if (aDDay != bDDay) return aDDay ? -1 : 1;

                    boolean aOver = (ad < 0); // D+N
                    boolean bOver = (bd < 0);
                    if (aOver != bOver) return aOver ? -1 : 1;

                    if (aOver) return Integer.compare(Math.abs(ad), Math.abs(bd)); // D+1이 위
                    return Integer.compare(ad, bd); // D-1이 위
                })
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

        //DailyRecordValue(자식) -> DailyRecord(부모) 삭제
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


}
