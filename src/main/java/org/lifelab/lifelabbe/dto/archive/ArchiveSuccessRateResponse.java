package org.lifelab.lifelabbe.dto.archive;

import java.time.LocalDate;
import java.util.List;

public record ArchiveSuccessRateResponse(
        List<Item> experiments
) {
    public record Item(
            String experimentId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            boolean isSuccess,
            int successRate
    ) {}
}
