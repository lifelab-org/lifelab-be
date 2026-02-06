package org.lifelab.lifelabbe.dto.experiment;

import lombok.Getter;
import org.lifelab.lifelabbe.domain.TodayRecordOutcome;

@Getter
public class TodayRecordOutcomeResponse {

    private final Long experimentId;
    private final TodayRecordOutcome outcome;
    private final String step;

    public TodayRecordOutcomeResponse(
            Long experimentId,
            TodayRecordOutcome outcome,
            String step
    ) {
        this.experimentId = experimentId;
        this.outcome = outcome;
        this.step = step;
    }
}
