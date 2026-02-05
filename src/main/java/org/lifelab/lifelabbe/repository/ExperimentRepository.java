package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findByUserIdAndStatusOrderByEndDateAsc(
            Long userId,
            ExperimentStatus status
    );
}
