package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    Optional<DailyRecord> findByExperiment_IdAndRecordDate(
            Long experimentId,
            LocalDate recordDate
    );
}
