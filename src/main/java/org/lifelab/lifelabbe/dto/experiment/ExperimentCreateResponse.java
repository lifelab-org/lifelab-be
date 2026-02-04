package org.lifelab.lifelabbe.dto.experiment;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ExperimentCreateResponse(
        Long experimentId,
        String title,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int currentDay,
        List<RecordItemDto> recordItems
) {
    @Builder
    public record RecordItemDto(Long recordItemId, String name) {}
}
