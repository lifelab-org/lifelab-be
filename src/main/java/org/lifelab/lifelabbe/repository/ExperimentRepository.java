package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {}
