package com.county.vision.repo;

import com.county.vision.entity.ParentConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParentConfirmationRepository extends JpaRepository<ParentConfirmation, Long> {
    Optional<ParentConfirmation> findByScreeningId(Long screeningId);
    Optional<ParentConfirmation> findByStudentIdAndStatus(Long studentId, String status);
    long countByStatus(String status);
    long countBySchoolReviewStatus(String status);
}
