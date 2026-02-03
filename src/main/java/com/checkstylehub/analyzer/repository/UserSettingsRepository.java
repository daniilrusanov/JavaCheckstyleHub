package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserSettings entity operations.
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    /**
     * Find settings by user ID.
     */
    Optional<UserSettings> findByUserId(Long userId);

    /**
     * Check if settings exist for user.
     */
    boolean existsByUserId(Long userId);
}
