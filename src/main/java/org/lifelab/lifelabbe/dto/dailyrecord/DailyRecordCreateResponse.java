package org.lifelab.lifelabbe.dto.dailyrecord;

import java.time.LocalDate;
import java.util.List;

public record DailyRecordCreateResponse(
        Long experimentId,
        Long dailyRecordId,
        LocalDate recordDate,
        DailyRecordCreateRequest.Outcome outcome,
        List<Value> values
) {
    public record Value(String recordItemKey, Integer value) {}
}
