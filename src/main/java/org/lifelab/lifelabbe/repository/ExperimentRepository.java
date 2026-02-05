package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    // 홈2 (ONGOING)
    List<Experiment> findByUserIdAndStatusOrderByEndDateAsc(Long userId, ExperimentStatus status);

    // 홈3 (UPCOMING)
    List<Experiment> findByUserIdAndStatusOrderByStartDateAsc(Long userId, ExperimentStatus status);

    Optional<Experiment> findByIdAndUserId(Long id, Long userId);
}
