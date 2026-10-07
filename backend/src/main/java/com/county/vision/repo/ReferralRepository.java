package com.county.vision.repo;

import com.county.vision.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findByStudentIdOrderByIssuedAtDesc(Long studentId);
    List<Referral> findByStatus(String status);
    List<Referral> findByTargetOrgIdAndStatus(Long targetOrgId, String status);
    Optional<Referral> findFirstByStudentIdAndStatusOrderByIssuedAtDesc(Long studentId, String status);
}
