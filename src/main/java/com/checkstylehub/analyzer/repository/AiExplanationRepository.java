package com.checkstylehub.analyzer.repository;

import com.checkstylehub.analyzer.entity.AiExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiExplanationRepository extends JpaRepository<AiExplanation, Long> {

    Optional<AiExplanation> findByAnalysisResultId(Long resultId);
}
