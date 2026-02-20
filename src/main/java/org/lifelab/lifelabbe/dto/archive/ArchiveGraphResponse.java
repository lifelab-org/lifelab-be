package org.lifelab.lifelabbe.dto.archive;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArchiveGraphResponse {
    private Long experimentId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<MetricSeries> metrics;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricSeries {
        private String name;
        private List<Point> points;       // 날짜별 값
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Point {
        private LocalDate date;
        private Integer value;            // null 허용 가능
    }
}