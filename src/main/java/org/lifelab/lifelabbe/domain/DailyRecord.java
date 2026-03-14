package org.lifelab.lifelabbe.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_record_experiment_date",
                columnNames = {"experiment_id", "record_date"}
        )
)
public class DailyRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyOutcome outcome;

    public enum DailyOutcome { SUCCESS, FAIL }

    private DailyRecord(Experiment experiment, LocalDate recordDate, DailyOutcome outcome) {
        this.experiment = experiment;
        this.recordDate = recordDate;
        this.outcome = outcome;
    }

    public static DailyRecord of(Experiment experiment, LocalDate recordDate, DailyOutcome outcome) {
        return new DailyRecord(experiment, recordDate, outcome);
    }
}
