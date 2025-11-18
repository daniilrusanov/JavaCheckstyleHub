package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.CheckstyleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for CheckstyleConfiguration entity.
 * Manages stored Checkstyle XML configurations.
 */
@Repository
public interface CheckstyleConfigurationRepository extends JpaRepository<CheckstyleConfiguration, Long> {

    Optional<CheckstyleConfiguration> findByIsActiveTrue();

    Optional<CheckstyleConfiguration> findByConfigName(String configName);
}

