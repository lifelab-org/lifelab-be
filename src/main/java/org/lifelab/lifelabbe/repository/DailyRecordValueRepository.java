package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.DailyRecordValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
public interface DailyRecordValueRepository extends JpaRepository<DailyRecordValue, Long> {
    void deleteByDailyRecord_Id(Long dailyRecordId);

    //지표변화량 조회용: 실험 기간 평균값
    @Query("""
        select avg(v.value)
        from DailyRecordValue v
        join v.dailyRecord dr
        where dr.experiment.id = :experimentId
          and v.recordItemKey = :recordItemKey
          and dr.recordDate between :startDate and :endDate
    """)
    Optional<Double> findAvgValue(
            @Param("experimentId") Long experimentId,
            @Param("recordItemKey") String recordItemKey,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
