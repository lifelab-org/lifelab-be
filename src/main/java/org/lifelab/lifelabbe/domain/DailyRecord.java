package org.lifelab.lifelabbe.domain;

import org.lifelab.lifelabbe.domain.TodayRecordOutcome;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "daily_records",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"experiment_id", "record_date"})
        }
)
public class DailyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodayRecordOutcome outcome;

    public DailyRecord(
            Experiment experiment,
            LocalDate recordDate,
            TodayRecordOutcome outcome
    ) {
        this.experiment = experiment;
        this.recordDate = recordDate;
        this.outcome = outcome;
    }
}
