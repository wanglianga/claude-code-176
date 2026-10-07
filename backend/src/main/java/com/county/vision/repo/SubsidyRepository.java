package com.county.vision.repo;

import com.county.vision.entity.Subsidy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubsidyRepository extends JpaRepository<Subsidy, Long> {
    List<Subsidy> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    Optional<Subsidy> findFirstByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Subsidy> findByStatusOrderByCreatedAtDesc(String status);
}
