package com.checkstylehub.analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stores the AI-generated explanation for a single analysis violation.
 * Linked one-to-one with {@link AnalysisResult}.
 */
@Entity
@Table(name = "ai_explanations")
@Getter
@Setter
public class AiExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false, unique = true)
    private AnalysisResult analysisResult;

    /** Markdown-formatted explanation produced by the local LLM. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExperienceLevel experienceLevel;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AiExplanation() {
        this.createdAt = LocalDateTime.now();
    }
}
