package com.opticine.repository;

import com.opticine.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findAllByOrderByMinSpentAsc();
    Optional<Membership> findFirstByMinSpentLessThanEqualOrderByMinSpentDesc(BigDecimal totalSpent);
}
