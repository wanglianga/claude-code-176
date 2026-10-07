package com.county.vision.repo;

import com.county.vision.entity.ScreeningBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScreeningBatchRepository extends JpaRepository<ScreeningBatch, Long> {
    List<ScreeningBatch> findBySchoolIdOrderByScreeningDateDesc(Long schoolId);
}
