package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {
    boolean existsByExperimentIdAndRecordDate(Long experimentId, LocalDate recordDate);
    boolean existsByExperimentId(Long experimentId);

    List<DailyRecord> findByExperimentId(Long experimentId);

    void deleteByExperimentId(Long experimentId);
}
