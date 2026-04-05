package com.checkstylehub.analyzer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing an analysis request for a Git repository.
 * Contains status, error information, and relationships to results and logs.
 */
@Entity
@Table(name = "analysis_requests")
@Getter
@Setter
public class AnalysisRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Quality score 0–100 from thesis formula (TDI / LOC → DD → QS). */
    @Column
    private Integer qualityScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AnalysisResult> results;

    public AnalysisRequest() {
        this.createdAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
    }

    public AnalysisRequest(String repoUrl) {
        this();
        this.repoUrl = repoUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnalysisRequest that = (AnalysisRequest) o;
        return Objects.equals(id, that.id) && Objects.equals(repoUrl, that.repoUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, repoUrl);
    }

    /**
     * Represents the lifecycle status of an analysis request.
     */
    public enum RequestStatus {
        PENDING,
        CLONING,
        ANALYZING,
        COMPLETED,
        FAILED
    }
}
