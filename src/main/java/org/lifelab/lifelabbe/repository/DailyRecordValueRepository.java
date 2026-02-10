package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.DailyRecordValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyRecordValueRepository extends JpaRepository<DailyRecordValue, Long> {
}
