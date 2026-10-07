package com.county.vision.repo;

import com.county.vision.entity.RevisitDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RevisitDiagnosisRepository extends JpaRepository<RevisitDiagnosis, Long> {
    List<RevisitDiagnosis> findByStudentIdOrderByVisitDateDesc(Long studentId);
    Optional<RevisitDiagnosis> findFirstByStudentIdOrderByVisitDateDesc(Long studentId);
    List<RevisitDiagnosis> findByOrgIdOrderByVisitDateDesc(Long orgId);
    long countBySubsidyEligibleTrue();
}
