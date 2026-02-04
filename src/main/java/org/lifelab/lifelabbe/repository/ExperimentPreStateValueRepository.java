package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.ExperimentPreStateValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentPreStateValueRepository extends JpaRepository<ExperimentPreStateValue, Long> {

    // 가장 안전한 네이밍 (experiment.id로 탐색)
    boolean existsByExperiment_Id(Long experimentId);
}
