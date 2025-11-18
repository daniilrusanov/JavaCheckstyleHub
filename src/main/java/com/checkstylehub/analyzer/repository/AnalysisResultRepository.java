package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    java.util.List<AnalysisResult> findByRequestId(Long requestId);
}
