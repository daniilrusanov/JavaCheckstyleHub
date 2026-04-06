package com.checkstylehub.analyzer.service;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Computes Total Defect Index (TDI), Defect Density (DD), and Quality Score (QS) per thesis formula.
 * <ul>
 *   <li>Checkstyle: INFO = 1, WARNING = 3, ERROR = 5 (IGNORE = 0)</li>
 *   <li>PMD: LOW = 4, MEDIUM* = 8, HIGH = 15</li>
 *   <li>Compilation error (if any): +50</li>
 *   <li>DD = (TDI / LOC) × 1000 (LOC ≥ 1)</li>
 *   <li>QS = round(100 × e^(−0.005 × DD)), clamped to [0, 100]</li>
 * </ul>
 */
@Service
public class MetricsCalculationService {

    public long countLinesOfCode(List<Path> javaFiles) throws IOException {
        long total = 0;
        for (Path p : javaFiles) {
            try (Stream<String> lines = Files.lines(p)) {
                total += lines.count();
            }
        }
        return total;
    }

    /**
     * Sums weighted defect points from Checkstyle, PMD, and optional compilation failure.
     */
    public long computeTotalDefectIndex(
            List<AuditEvent> checkstyleEvents,
            List<PmdService.PmdViolation> pmdViolations,
            boolean compilationError) {
        long tdi = 0;
        if (checkstyleEvents != null) {
            for (AuditEvent e : checkstyleEvents) {
                tdi += pointsForCheckstyleSeverity(e.getSeverityLevel().getName());
            }
        }
        if (pmdViolations != null) {
            for (PmdService.PmdViolation v : pmdViolations) {
                tdi += pointsForPmdPriority(v.severity());
            }
        }
        if (compilationError) {
            tdi += 50;
        }
        return tdi;
    }

    public double computeDefectDensity(long totalDefectIndex, long linesOfCode) {
        long loc = Math.max(1L, linesOfCode);
        return (totalDefectIndex / (double) loc) * 1000.0;
    }

    public int computeQualityScoreFromDefectDensity(double defectDensity) {
        double qs = 100.0 * Math.exp(-0.005 * defectDensity);
        int rounded = (int) Math.round(qs);
        return Math.max(0, Math.min(100, rounded));
    }

    public int computeQualityScore(
            List<AuditEvent> checkstyleEvents,
            List<PmdService.PmdViolation> pmdViolations,
            long linesOfCode,
            boolean compilationError) {
        long tdi = computeTotalDefectIndex(checkstyleEvents, pmdViolations, compilationError);
        double dd = computeDefectDensity(tdi, linesOfCode);
        return computeQualityScoreFromDefectDensity(dd);
    }

    static int pointsForCheckstyleSeverity(String severityName) {
        if (severityName == null) {
            return 0;
        }
        String s = severityName.toUpperCase(Locale.ROOT);
        return switch (s) {
            case "INFO" -> 1;
            case "WARNING" -> 3;
            case "ERROR" -> 5;
            case "IGNORE" -> 0;
            default -> 0;
        };
    }

    static int pointsForPmdPriority(String priorityName) {
        if (priorityName == null) {
            return 0;
        }
        String p = priorityName.toUpperCase(Locale.ROOT);
        return switch (p) {
            case "HIGH" -> 15;
            case "MEDIUM", "MEDIUM_LOW", "MEDIUM_HIGH" -> 8;
            case "LOW" -> 4;
            default -> 8;
        };
    }
}
