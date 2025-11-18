package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {
}
