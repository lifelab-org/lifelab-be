package org.lifelab.lifelabbe.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "record_items")
public class RecordItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(nullable = false, length = 50)
    private String name;

    public RecordItem(String name) {
        this.name = name;
    }

    void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }
}
