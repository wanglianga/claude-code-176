package com.county.vision.domain.repository;

import com.county.vision.domain.entity.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {
    List<Intervention> findByStudentIdOrderByExecDateDescIdDesc(Long studentId);
}
