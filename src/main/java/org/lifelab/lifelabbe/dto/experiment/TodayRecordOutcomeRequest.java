package org.lifelab.lifelabbe.dto.experiment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.lifelab.lifelabbe.domain.TodayRecordOutcome;

@Getter
public class TodayRecordOutcomeRequest {

    @NotNull
    private TodayRecordOutcome outcome;
}

