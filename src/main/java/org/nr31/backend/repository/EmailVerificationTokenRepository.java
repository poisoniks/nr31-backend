package org.nr31.backend.repository;

import org.nr31.backend.model.EmailVerificationToken;
import org.nr31.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    int deleteAllByExpiryDateBefore(Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EmailVerificationToken e where e.token = :token")
    void deleteByTokenCustom(@Param("token") String token);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EmailVerificationToken e where e.user = :user")
    void deleteByUser(@Param("user") User user);

    Optional<EmailVerificationToken> findByUser(User user);
}
