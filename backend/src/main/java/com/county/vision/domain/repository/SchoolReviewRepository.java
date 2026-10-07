package com.county.vision.domain.repository;

import com.county.vision.domain.entity.SchoolReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolReviewRepository extends JpaRepository<SchoolReview, Long> {
    Optional<SchoolReview> findFirstByStudentIdOrderByIdDesc(Long studentId);
}
