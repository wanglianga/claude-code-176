package com.county.vision.repo;

import com.county.vision.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findByOrganizationId(Long orgId);
    List<AppUser> findByRole(String role);
    List<AppUser> findByRoleAndOrganizationId(String role, Long orgId);
}
