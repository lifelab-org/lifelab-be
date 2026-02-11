package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {
    boolean existsByExperimentIdAndRecordDate(Long experimentId, LocalDate recordDate);
}
