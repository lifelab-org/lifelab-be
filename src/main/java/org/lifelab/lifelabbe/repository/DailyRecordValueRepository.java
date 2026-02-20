package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.DailyRecordValue;
import org.lifelab.lifelabbe.repository.projection.DailyValueRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
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

    @Query("""
        select
            v.recordItemKey,
            dr.recordDate,
            v.value
        from DailyRecordValue v
        join v.dailyRecord dr
        where dr.experiment.id = :experimentId
          and dr.recordDate between :from and :to
        order by v.recordItemKey asc, dr.recordDate asc
    """)
    //그래프(라인차트)용: "기록된 것만" 날짜별 값 리스트 조회
    List<Object[]> findGraphRowsRaw(
            @Param("experimentId") Long experimentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
    select
        v.recordItemKey as recordItemKey,
        dr.recordDate as recordDate,
        v.value as value
    from DailyRecordValue v
    join v.dailyRecord dr
    where dr.experiment.id = :experimentId
""")
    List<DailyValueRow> findAllDailyValuesByExperimentId(@Param("experimentId") Long experimentId);
}
