package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.Experiment;
import org.lifelab.lifelabbe.domain.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    // status 기반 조회
    List<Experiment> findByUserIdAndStatusAndResultCheckedFalseOrderByEndDateAsc(
            Long userId,
            ExperimentStatus status
    );

    List<Experiment> findByUserIdAndStatusOrderByStartDateAsc(
            Long userId,
            ExperimentStatus status
    );
    //아카이브: COMPLETED만 (최신 종료일 순)
    List<Experiment> findByUserIdAndStatusOrderByEndDateDesc(
            Long userId,
            ExperimentStatus status
    );
    // 날짜 기반 조회 - 홈2
    // startDate <= today AND resultChecked=false
    List<Experiment> findByUserIdAndStartDateLessThanEqualAndResultCheckedFalseOrderByEndDateAsc(
            Long userId,
            LocalDate today
    );

    //날짜 기반 조회 - 홈3
    List<Experiment> findByUserIdAndStartDateAfterOrderByStartDateAsc(
            Long userId,
            LocalDate today
    );

    Optional<Experiment> findByIdAndUserId(Long id, Long userId);

    // 상태 자동 업데이트

    // 1) ONGOING -> COMPLETED (endDate < today)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Experiment e
           set e.status = org.lifelab.lifelabbe.domain.ExperimentStatus.COMPLETED
         where e.status = org.lifelab.lifelabbe.domain.ExperimentStatus.ONGOING
           and e.resultChecked = true
    """)
    int updateOngoingToCompleted(@Param("today") LocalDate today);

    // 2) UPCOMING -> ONGOING (startDate <= today <= endDate)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Experiment e
           set e.status = org.lifelab.lifelabbe.domain.ExperimentStatus.ONGOING
         where e.status = org.lifelab.lifelabbe.domain.ExperimentStatus.UPCOMING
           and e.startDate <= :today
           and e.endDate >= :today
    """)
    int updateUpcomingToOngoing(@Param("today") LocalDate today);
}
