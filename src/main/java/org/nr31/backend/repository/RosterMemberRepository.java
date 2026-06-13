package org.nr31.backend.repository;

import org.nr31.backend.model.RosterMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RosterMemberRepository extends JpaRepository<RosterMember, Long> {
    Optional<RosterMember> findByMbNickname(String mbNickname);
    Optional<RosterMember> findByDiscordId(String discordId);
}
