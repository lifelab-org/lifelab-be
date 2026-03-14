package org.lifelab.lifelabbe.dto.experiment;

import java.util.List;

public record ExperimentCreateRecordItemsResponse(
        Long experimentId,
        List<RecordItemKeyResponse> values
) {

    public record RecordItemKeyResponse(
            String recordItemKey
    ) {}

}
