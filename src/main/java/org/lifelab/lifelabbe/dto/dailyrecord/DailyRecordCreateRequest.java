package org.lifelab.lifelabbe.dto.dailyrecord;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DailyRecordCreateRequest(
        @NotNull Outcome outcome,
        @NotEmpty @Size(min = 1) List<Value> values
) {
    public enum Outcome { SUCCESS, FAIL }

    public record Value(
            @NotNull String recordItemKey,
            @NotNull Integer value
    ) {}
}
