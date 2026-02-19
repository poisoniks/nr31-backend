package org.nr31.backend.repository;

import jakarta.persistence.QueryHint;
import org.nr31.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @QueryHints(@QueryHint(name = org.hibernate.jpa.AvailableHints.HINT_CACHEABLE, value = "true"))
    Optional<User> findByUsername(String username);
}
