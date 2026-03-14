package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.DailyRecord;
import org.lifelab.lifelabbe.domain.DailyRecord.DailyOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {
    boolean existsByExperimentIdAndRecordDate(Long experimentId, LocalDate recordDate);
    boolean existsByExperimentId(Long experimentId);

    List<DailyRecord> findByExperimentId(Long experimentId);

    void deleteByExperimentId(Long experimentId);

    //성공/실패 일수 집계
    long countByExperiment_IdAndOutcome(Long experimentId, DailyOutcome outcome);
    //출석률(기록한 일수)
    long countByExperimentId(Long experimentId);
    long countByExperiment_IdAndRecordDateBetween(Long experimentId, LocalDate start, LocalDate end);
    //today 이전 가장 최근 기록일(=직전 기록일)
    @Query("""
        select max(dr.recordDate)
        from DailyRecord dr
        where dr.experiment.id = :experimentId
          and dr.recordDate < :today
    """)
    Optional<LocalDate> findLatestRecordDateBefore(
            @Param("experimentId") Long experimentId,
            @Param("today") LocalDate today
    );
}
