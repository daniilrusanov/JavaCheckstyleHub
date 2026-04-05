package com.checkstylehub.analyzer.dto;

import com.checkstylehub.analyzer.entity.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis cache payload: analysis results plus metrics needed to restore quality score without re-cloning.
 */
public class CachedAnalysisBundle {

    private List<AnalysisResult> results = new ArrayList<>();
    private long linesOfCode;
    private Integer qualityScore;

    public List<AnalysisResult> getResults() {
        return results;
    }

    public void setResults(List<AnalysisResult> results) {
        this.results = results != null ? results : new ArrayList<>();
    }

    public long getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(long linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }
}
