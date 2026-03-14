package org.lifelab.lifelabbe.dto.archive;

import java.util.List;

public record ArchiveMetricDeltaResponse(
        Long experimentId,
        List<Metric> metrics
) {
    public record Metric(
            String name,
            double previousValue,
            double currentValue,
            double delta,
            ChangeDirection direction
    ) {}
}