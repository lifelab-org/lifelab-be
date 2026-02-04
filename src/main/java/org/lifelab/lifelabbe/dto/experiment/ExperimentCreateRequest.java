package org.lifelab.lifelabbe.dto.experiment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ExperimentCreateRequest(
        @NotBlank String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String rule,
        @NotEmpty @Valid List<RecordItemCreate> recordItems
) {
    public record RecordItemCreate(@NotBlank String name) {}
}
