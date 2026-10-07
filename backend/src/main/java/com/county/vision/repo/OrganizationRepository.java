package com.county.vision.repo;

import com.county.vision.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    List<Organization> findByOrgType(String orgType);
    List<Organization> findByOrgTypeIn(List<String> types);
    List<Organization> findByArea(String area);
}
