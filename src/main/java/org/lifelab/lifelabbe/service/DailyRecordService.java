package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.DailyRecord;
import org.lifelab.lifelabbe.domain.DailyRecordValue;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.dto.dailyrecord.DailyRecordCreateRequest;
import org.lifelab.lifelabbe.dto.dailyrecord.DailyRecordCreateResponse;
import org.lifelab.lifelabbe.dto.dailyrecord.DailyRecordItemsResponse;
import org.lifelab.lifelabbe.repository.DailyRecordRepository;
import org.lifelab.lifelabbe.repository.DailyRecordValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentPreStateValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyRecordService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentPreStateValueRepository preStateValueRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final DailyRecordValueRepository dailyRecordValueRepository;

    public DailyRecordCreateResponse create(Long userId, Long experimentId, DailyRecordCreateRequest req) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 실험 존재 + 소유권 확인
        Experiment experiment = experimentRepository
                .findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));
        if (req == null || req.values() == null || req.values().isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }
        // 오늘 기록 이미 존재하면 409
        if (dailyRecordRepository.existsByExperimentIdAndRecordDate(experimentId, today)) {
            throw new GlobalException(ErrorCode.REC_409);
        }

        // 실험 전 상태에서 요구되는 key 목록 가져오기
        List<String> allowedKeys = preStateValueRepository
                .findRecordItemKeysByExperimentId(experimentId);

        if (allowedKeys == null || allowedKeys.isEmpty()) {
            throw new GlobalException(ErrorCode.PRE_STATE_404);
        }

        // allowedSet 만들기
        Set<String> allowedSet = new HashSet<>();
        for (String k : allowedKeys) {
            String key = normalizeKey(k);
            if (key == null || key.isBlank()) continue;
            allowedSet.add(key);
        }

        if (allowedSet.isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        // 요청 검증 (전부 체크 필수)
        Set<String> seen = new HashSet<>();

        for (DailyRecordCreateRequest.Value v : req.values()) {
            String key = normalizeKey(v.recordItemKey());
            Integer value = v.value();

            if (key == null || key.isBlank() || value == null) {
                throw new GlobalException(ErrorCode.INVALID_PARAMETER);
            }

            // key가 이 실험의 preState 목록에 포함되어야 함
            if (!allowedSet.contains(key)) {
                throw new GlobalException(ErrorCode.PRE_STATE_ITEMS_MISMATCH);
            }

            // 중복 제출 방지
            if (!seen.add(key)) {
                throw new GlobalException(ErrorCode.INVALID_PARAMETER);
            }
        }

        // 전부 체크 필수
        if (!seen.equals(allowedSet)) {
            throw new GlobalException(ErrorCode.PRE_STATE_ITEMS_MISMATCH);
        }

        // DailyRecord 저장
        DailyRecord.DailyOutcome outcome = mapOutcome(req.outcome());

        DailyRecord dailyRecord = DailyRecord.of(experiment, today, outcome);
        dailyRecordRepository.save(dailyRecord);

        // 값 저장
        List<DailyRecordValue> saved = new ArrayList<>();
        for (DailyRecordCreateRequest.Value v : req.values()) {
            saved.add(
                    DailyRecordValue.of(
                            dailyRecord,
                            normalizeKey(v.recordItemKey()),
                            v.value()
                    )
            );
        }
        dailyRecordValueRepository.saveAll(saved);

        // 응답 생성
        List<DailyRecordCreateResponse.Value> resp = saved.stream()
                .map(x -> new DailyRecordCreateResponse.Value(x.getRecordItemKey(), x.getValue()))
                .toList();

        return new DailyRecordCreateResponse(
                experimentId,
                dailyRecord.getId(),
                today,
                req.outcome(),
                resp
        );
    }
    //오늘의 기록 항목 조회
    @Transactional(readOnly = true)
    public DailyRecordItemsResponse getItems(Long userId, Long experimentId) {

        // 실험 존재 + 소유권 확인
        experimentRepository.findByIdAndUserId(experimentId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        // preStateValue에서 key 목록만 조회
        List<String> keys = preStateValueRepository.findRecordItemKeysByExperimentId(experimentId);

        // pre-state 자체가 없으면 404
        if (keys == null || keys.isEmpty()) {
            throw new GlobalException(ErrorCode.PRE_STATE_404);
        }

        List<DailyRecordItemsResponse.Item> items = keys.stream()
                .map(this::normalizeKey)
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .map(DailyRecordItemsResponse.Item::new)
                .toList();

        // 유효한 key가 하나도 없으면 파라미터 오류로 처리
        if (items.isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        return new DailyRecordItemsResponse(experimentId, items);
    }

    private DailyRecord.DailyOutcome mapOutcome(DailyRecordCreateRequest.Outcome outcome) {
        if (outcome == null) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        return (outcome == DailyRecordCreateRequest.Outcome.SUCCESS)
                ? DailyRecord.DailyOutcome.SUCCESS
                : DailyRecord.DailyOutcome.FAIL;
    }

    private String normalizeKey(String key) {
        return key == null ? null : key.trim();
    }
}
