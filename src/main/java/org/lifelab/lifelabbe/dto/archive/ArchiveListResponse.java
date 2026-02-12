package org.lifelab.lifelabbe.dto.archive;

import java.time.LocalDate;
import java.util.List;

public record ArchiveListResponse(
        List<ArchiveExperimentDto> experiments
) {
    public record ArchiveExperimentDto(
            String experimentId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            boolean isSuccess
    ) {}
}
