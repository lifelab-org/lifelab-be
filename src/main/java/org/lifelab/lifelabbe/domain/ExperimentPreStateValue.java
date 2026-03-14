package org.lifelab.lifelabbe.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "experiment_pre_state_value",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pre_state_value_experiment_record_item",
                columnNames = {"experiment_id", "record_item_key"}
        )
)
public class ExperimentPreStateValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    // 실험 상태전 기록
    @Column(name = "record_item_key", nullable = false, length = 50)
    private String recordItemKey;

    @Column(nullable = false)
    private Integer value; // 1~7

    protected ExperimentPreStateValue() {}

    public ExperimentPreStateValue(
            Experiment experiment,
            String recordItemKey,
            Integer value
    ) {
        this.experiment = experiment;
        this.recordItemKey = recordItemKey;
        this.value = value;
    }

    // static factory
    public static ExperimentPreStateValue of(
            Experiment experiment,
            String recordItemKey,
            Integer value
    ) {
        return new ExperimentPreStateValue(experiment, recordItemKey, value);
    }

    public Long getId() { return id; }
    public Experiment getExperiment() { return experiment; }
    public String getRecordItemKey() { return recordItemKey; }
    public Integer getValue() { return value; }
}
