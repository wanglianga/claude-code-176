package com.county.vision.domain.repository;

import com.county.vision.domain.entity.GlassesRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlassesRecordRepository extends JpaRepository<GlassesRecord, Long> {
    List<GlassesRecord> findByStudentIdOrderByGlassesDateAscIdAsc(Long studentId);
    long countByPartnerOpticianFalse();
    long countByDiscomfortTrueAndDiscomfortResolvedFalse();
}
