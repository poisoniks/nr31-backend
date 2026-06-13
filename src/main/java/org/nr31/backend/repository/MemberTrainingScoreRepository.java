package org.nr31.backend.repository;

import org.nr31.backend.model.MemberTrainingScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberTrainingScoreRepository extends JpaRepository<MemberTrainingScore, Long> {
    Optional<MemberTrainingScore> findByMemberIdAndDisciplineId(Long memberId, Long disciplineId);
}
