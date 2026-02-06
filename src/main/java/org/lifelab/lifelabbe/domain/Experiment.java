package org.lifelab.lifelabbe.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "experiments")
public class Experiment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 쿠키 인증으로 나온 우리 서비스 유저 ID

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String rule;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperimentStatus status;

    @Column(nullable = false)
    private boolean resultChecked = false;

    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecordItem> recordItems = new ArrayList<>();

    @Builder
    public Experiment(Long userId, String title, String rule, LocalDate startDate, LocalDate endDate, ExperimentStatus status) {
        this.userId = userId;
        this.title = title;
        this.rule = rule;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.resultChecked = false;
    }

    public void addRecordItem(RecordItem item) {
        recordItems.add(item);
        item.setExperiment(this);
    }

    public int totalDaysInclusive() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public void markResultChecked() {
        this.resultChecked = true;
    }

    public void markCompleted() {
        this.status = ExperimentStatus.COMPLETED;
    }

}
