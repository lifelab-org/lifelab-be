package org.lifelab.lifelabbe.repository.projection;

import java.time.LocalDate;

public interface DailyValueRow {
    String getRecordItemKey();   // 지표명
    LocalDate getRecordDate();   // 날짜
    Integer getValue();          // 점수(정수 스케일 가정)
}