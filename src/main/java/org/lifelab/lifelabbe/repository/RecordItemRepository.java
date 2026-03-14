package org.lifelab.lifelabbe.repository;

import org.lifelab.lifelabbe.domain.RecordItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordItemRepository extends JpaRepository<RecordItem, Long> {
    void deleteByExperiment_Id(Long experimentId);
}
