package org.lifelab.lifelabbe.dto.ai;

public record MetricChangeDto(
        String key,
        float before,
        float after,
        String polarity,
        String unit
) {}