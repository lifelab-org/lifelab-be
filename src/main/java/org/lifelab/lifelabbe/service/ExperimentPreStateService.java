package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentPreStateValue;
import org.lifelab.lifelabbe.domain.RecordItem;
import org.lifelab.lifelabbe.dto.experiment.PreStateRequest;
import org.lifelab.lifelabbe.dto.experiment.PreStateResponse;
import org.lifelab.lifelabbe.repository.ExperimentPreStateValueRepository;
import org.lifelab.lifelabbe.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExperimentPreStateService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentPreStateValueRepository preStateValueRepository;

    @Transactional
    public PreStateResponse savePreState(Long experimentId, Long userId, PreStateRequest request) {
        // 실험 존재 여부 확인
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.EXP_404));

        // 실험 소유자 검증
        if (!experiment.getUserId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN);
        }

        // 실험 전 상태는 실험당 1회만 허용
        if (preStateValueRepository.existsByExperiment_Id(experimentId)) {
            throw new GlobalException(ErrorCode.PRE_STATE_409);
        }

        // 요청 값 검증
        if (request == null || request.getValues() == null || request.getValues().isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        // 실험에서 허용된 기록 항목(key) 집합
        Set<String> allowedKeys = new HashSet<>();
        for (RecordItem ri : experiment.getRecordItems()) {
            if (ri != null && ri.getName() != null) {
                allowedKeys.add(ri.getName().trim());
            }
        }

        //실험에 기록항목이 아예 없으면 저장 불가
        if (allowedKeys.isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        List<ExperimentPreStateValue> values = new ArrayList<>();
        Set<String> requestedKeys = new HashSet<>();

        // 개별 항목 검증 + 저장 객체 생성
        for (PreStateRequest.ValueItem item : request.getValues()) {
            if (item == null || item.getRecordItemId() == null || item.getValue() == null) {
                throw new GlobalException(ErrorCode.INVALID_PARAMETER);
            }

            String key = item.getRecordItemId().trim(); // 문자열 키(예: "피로도")
            if (key.isEmpty()) {
                throw new GlobalException(ErrorCode.INVALID_PARAMETER);
            }

            // 같은 요청에서 중복 키 방지
            if (!requestedKeys.add(key)) {
                throw new GlobalException(ErrorCode.INVALID_PARAMETER);
            }

            // 요청된 key가 "해당 실험의 기록항목"과 일치하는지 검증
            if (!allowedKeys.contains(key)) {
                // ErrorCode를 더 세분화하고 싶으면 PRE_STATE_ITEM_NOT_IN_EXPERIMENT 같은 걸 추가 추천
                throw new GlobalException(ErrorCode.PRE_STATE_ITEMS_MISMATCH);
            }

            values.add(ExperimentPreStateValue.of(experiment, key, item.getValue()));
        }

        // "완전 일치" 정책(누락도 불가)
        if (!requestedKeys.equals(allowedKeys)) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
         }

        // 일괄 저장
        preStateValueRepository.saveAll(values);

        return new PreStateResponse(experimentId, true);
    }
}
