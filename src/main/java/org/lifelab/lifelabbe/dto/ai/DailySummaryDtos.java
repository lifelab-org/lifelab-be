package org.lifelab.lifelabbe.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public class DailySummaryDtos {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AiDailySummaryGenerateRequest(
            String experimentName,
            String recordDate,
            Boolean hasTodayRecord,
            List<MetricDeltaDto> metrics,
            List<PreStateDto> preStates
    ) {}

    public record AiDailySummaryAiResponse(
            String status,
            String summary
    ) {}

    public record AiDailySummaryResponse(
            Long experimentId,
            String recordDate,
            String status,
            String summary
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MetricDeltaDto(
            String recordItemKey,
            float yesterday,
            float today,
            String polarity,
            String unit
    ) {}

    // 실험 전 상태 값
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PreStateDto(
            String recordItemKey,
            float value,
            String polarity,
            String unit
    ) {}
}