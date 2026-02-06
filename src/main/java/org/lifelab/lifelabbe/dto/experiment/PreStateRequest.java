package org.lifelab.lifelabbe.dto.experiment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class PreStateRequest {

    @NotNull
    private List<ValueItem> values;

    public List<ValueItem> getValues() { return values; }

    public static class ValueItem {
        @NotNull
        private String recordItemId;

        @NotNull
        @Min(1)
        @Max(7)
        private Integer value;

        public String getRecordItemId() { return recordItemId; }
        public Integer getValue() { return value; }
    }
}
