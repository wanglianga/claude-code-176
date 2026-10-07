package com.county.vision.domain.repository;

import com.county.vision.domain.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolRepository extends JpaRepository<School, Long> {
    List<School> findAllByOrderByIdAsc();
    List<School> findByRegionId(Long regionId);
}
