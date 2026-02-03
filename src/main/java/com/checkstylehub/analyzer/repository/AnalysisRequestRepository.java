package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {
    
    /**
     * Find all analysis requests by user ID, ordered by creation date descending.
     */
    List<AnalysisRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
