package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.ExperimentPreStateValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ExperimentPreStateValueRepository extends JpaRepository<ExperimentPreStateValue, Long> {

    // 가장 안전한 네이밍 (experiment.id로 탐색)
    boolean existsByExperiment_Id(Long experimentId);

    void deleteByExperiment_Id(Long experimentId);

    // ongoing 리스트용 (여러 실험 한 번에)
    @Query("""
        select distinct v.experiment.id
        from ExperimentPreStateValue v
        where v.experiment.userId = :userId
          and v.experiment.id in :experimentIds
    """)
    List<Long> findRecordedExperimentIds(
            @Param("userId") Long userId,
            @Param("experimentIds") Collection<Long> experimentIds
    );
    //일일기록 검증용: 이 실험에서 요구되는 recordItemKey 목록

    @Query("""
        select v.recordItemKey
        from ExperimentPreStateValue v
        where v.experiment.id = :experimentId
    """)
    List<String> findRecordItemKeysByExperimentId(@Param("experimentId") Long experimentId);
}
