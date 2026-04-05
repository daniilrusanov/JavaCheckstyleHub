package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.AnalysisResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    /**
     * Find all results by request ID.
     */
    List<AnalysisResult> findByRequestId(Long requestId);

    /**
     * Count results by request ID.
     */
    long countByRequestId(Long requestId);

    /**
     * Return (message, frequency) pairs for a request, most frequent first.
     * Used by FRS06 to build the general AI summary prompt.
     */
    @Query("SELECT r.message, COUNT(r) as freq FROM AnalysisResult r " +
           "WHERE r.request.id = :requestId GROUP BY r.message ORDER BY freq DESC")
    List<Object[]> findTopErrorsByRequestId(@Param("requestId") Long requestId, Pageable pageable);
}
