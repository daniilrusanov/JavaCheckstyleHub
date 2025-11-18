package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.AnalysisLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for AnalysisLog entity.
 * Provides database operations for log entries.
 */
@Repository
public interface AnalysisLogRepository extends JpaRepository<AnalysisLog, Long> {
    Page<AnalysisLog> findByRequestId(Long requestId, Pageable pageable);
}
