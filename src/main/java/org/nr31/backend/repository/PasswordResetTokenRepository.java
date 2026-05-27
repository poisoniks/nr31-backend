package org.nr31.backend.repository;

import org.nr31.backend.model.PasswordResetToken;
import org.nr31.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    int deleteAllByExpiryDateBefore(Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PasswordResetToken p where p.token = :token")
    void deleteByTokenCustom(@Param("token") String token);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PasswordResetToken p where p.user = :user")
    void deleteByUser(@Param("user") User user);

    Optional<PasswordResetToken> findByUser(User user);
}
