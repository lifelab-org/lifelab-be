package org.lifelab.lifelabbe.dto.archive;

import java.time.LocalDate;

public record TopMetricDeltaResponse(
        Long experimentId,
        String recordItemKey,
        Integer preValue,
        Integer valueAtMaxChange,   // 최대 변화 발생한 날의 값
        Integer delta,              // |value - pre|
        ChangeDirection direction, // UP/DOWN/SAME
        LocalDate recordDate       // 최대 변화 발생한 날짜
) {}