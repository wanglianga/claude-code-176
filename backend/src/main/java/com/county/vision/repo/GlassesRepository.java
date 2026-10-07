package com.county.vision.repo;

import com.county.vision.entity.Glasses;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GlassesRepository extends JpaRepository<Glasses, Long> {
    List<Glasses> findByStudentIdOrderByFittedDateDesc(Long studentId);
    List<Glasses> findByOrgId(Long orgId);
    List<Glasses> findBySource(String source);
}
