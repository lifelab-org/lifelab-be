package org.lifelab.lifelabbe.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_record_value",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_record_value_daily_key",
                columnNames = {"daily_record_id", "record_item_key"}
        )
)
public class DailyRecordValue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_record_id", nullable = false)
    private DailyRecord dailyRecord;

    @Column(name = "record_item_key", nullable = false, length = 50)
    private String recordItemKey;

    @Column(nullable = false)
    private Integer value;

    private DailyRecordValue(DailyRecord dailyRecord, String recordItemKey, Integer value) {
        this.dailyRecord = dailyRecord;
        this.recordItemKey = recordItemKey;
        this.value = value;
    }

    public static DailyRecordValue of(DailyRecord dailyRecord, String recordItemKey, Integer value) {
        return new DailyRecordValue(dailyRecord, recordItemKey, value);
    }
}
