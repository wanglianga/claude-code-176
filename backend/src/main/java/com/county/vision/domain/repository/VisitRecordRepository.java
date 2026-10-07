package com.county.vision.domain.repository;

import com.county.vision.domain.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {
    List<VisitRecord> findByStudentIdOrderByVisitDateAscIdAsc(Long studentId);
    long countByTrueMyopiaTrue();
}
