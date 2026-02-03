package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
