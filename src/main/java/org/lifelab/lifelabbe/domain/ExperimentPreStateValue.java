package org.lifelab.lifelabbe.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "experiment_pre_state_value",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pre_state_value_experiment_record_item",
                columnNames = {"experiment_id", "record_item_id"}
        )
)
public class ExperimentPreStateValue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_item_id", nullable = false)
    private RecordItem recordItem;

    @Column(nullable = false)
    private Integer value; // 1~7

    protected ExperimentPreStateValue() {}

    public ExperimentPreStateValue(Experiment experiment, RecordItem recordItem, Integer value) {
        this.experiment = experiment;
        this.recordItem = recordItem;
        this.value = value;
    }

    public Long getId() { return id; }
    public Experiment getExperiment() { return experiment; }
    public RecordItem getRecordItem() { return recordItem; }
    public Integer getValue() { return value; }
}
