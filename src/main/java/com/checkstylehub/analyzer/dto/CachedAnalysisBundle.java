package com.checkstylehub.analyzer.dto;

import com.checkstylehub.analyzer.entity.AnalysisResult;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis cache payload: analysis results plus metrics needed to restore quality score without re-cloning.
 */
@Data
@NoArgsConstructor
public class CachedAnalysisBundle {

    private List<AnalysisResult> results = new ArrayList<>();
    private long linesOfCode;
    private Integer qualityScore;
}
