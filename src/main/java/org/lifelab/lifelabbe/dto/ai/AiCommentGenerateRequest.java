package org.lifelab.lifelabbe.dto.ai;

import java.time.LocalDate;
import java.util.List;

public record AiCommentGenerateRequest(
        String experimentName,
        String startDate,
        String endDate,
        float attendanceRate,
        Float successRate,
        List<MetricChangeDto> metrics,
        String topChangedMetricKey
) {}