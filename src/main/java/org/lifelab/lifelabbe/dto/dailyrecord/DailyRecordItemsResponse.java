package org.lifelab.lifelabbe.dto.dailyrecord;

import java.util.List;

public record DailyRecordItemsResponse(
        Long experimentId,
        List<Item> values
) {
    public record Item(String recordItemKey) {}
}
