package org.nr31.backend.repository;

import org.nr31.backend.model.MemberAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberAwardRepository extends JpaRepository<MemberAward, Long> {
}
