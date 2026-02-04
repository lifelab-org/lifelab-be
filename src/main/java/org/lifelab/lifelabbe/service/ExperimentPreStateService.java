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
import org.lifelab.lifelabbe.repository.RecordItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentPreStateService {

    private final ExperimentRepository experimentRepository;
    private final RecordItemRepository recordItemRepository;
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
            throw new GlobalException(ErrorCode.REC_409);
        }
        // 요청 값 검증
        if (request == null || request.getValues() == null || request.getValues().isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_PARAMETER);
        }

        List<ExperimentPreStateValue> values = new ArrayList<>();
        // 개별 항목 검증
        for (PreStateRequest.ValueItem item : request.getValues()) {
            if (item == null || item.getRecordItemId() == null || item.getValue() == null) {
                throw new GlobalException(ErrorCode.INVALID_PARAMETER);
            }
            // recordItem 존재 확인
            RecordItem recordItem = recordItemRepository.findById(item.getRecordItemId())
                    .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_PARAMETER));
            // 실험 전 상태 값 생성
            values.add(new ExperimentPreStateValue(experiment, recordItem, item.getValue()));
        }
        // 일괄 저장
        preStateValueRepository.saveAll(values);
        return new PreStateResponse(experimentId, true);
    }
}
