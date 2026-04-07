package com.checkstylehub.analyzer.service;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Computes repository-relative file paths with Windows and multi-root filesystem compatibility.
 */
final class SafePathRelativizer {

    private SafePathRelativizer() {
    }

    /**
     * Computes a relative file path from base to other, with Windows compatibility.
     * Handles edge cases like different drive letters and filesystem roots.
     *
     * @param base  the repository root path
     * @param other the file path to relativize
     * @return relative path as string with forward slashes
     */
    static String relativize(Path base, Path other) {
        try {
            if (base == null || other == null) {
                return other == null ? "" : other.toString().replace('\\', '/');
            }

            Path baseAbs = base.toAbsolutePath().normalize();
            Path otherAbs = other.toAbsolutePath().normalize();

            boolean differentFs = !Objects.equals(baseAbs.getFileSystem(), otherAbs.getFileSystem());
            boolean differentRoot = (baseAbs.getRoot() == null && otherAbs.getRoot() != null)
                    || (baseAbs.getRoot() != null && !baseAbs.getRoot().equals(otherAbs.getRoot()));

            String baseStr = baseAbs.toString();
            String otherStr = otherAbs.toString();
            String baseStrLc = baseStr.toLowerCase(Locale.ROOT);
            String otherStrLc = otherStr.toLowerCase(Locale.ROOT);
            if (otherStrLc.startsWith(baseStrLc)) {
                String trimmed = otherStr.substring(baseStr.length());
                if (trimmed.startsWith("\\") || trimmed.startsWith("/")) {
                    trimmed = trimmed.substring(1);
                }
                String normalized = trimmed.replace('\\', '/');
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            }

            if (differentFs || differentRoot) {
                return relativizeDifferentRoot(baseAbs, otherAbs);
            }

            String rel = baseAbs.relativize(otherAbs).toString().replace('\\', '/');
            if (rel.startsWith("../") || rel.startsWith("..\\") || rel.contains(":\\") || rel.contains(":/")) {
                return relativizeWhenWalksUp(baseAbs, otherAbs, baseStr, otherStr, otherStrLc, baseStrLc);
            }
            return rel;
        } catch (IllegalArgumentException ex) {
            return relativizeFallback(base, other);
        }
    }

    private static String relativizeDifferentRoot(Path baseAbs, Path otherAbs) {
        String repoRootName = baseAbs.getFileName() != null ? baseAbs.getFileName().toString() : null;
        if (repoRootName != null) {
            int nameCount = otherAbs.getNameCount();
            for (int i = 0; i < nameCount; i++) {
                if (otherAbs.getName(i).toString().equalsIgnoreCase(repoRootName)) {
                    Path sub = otherAbs.subpath(i + 1, nameCount);
                    String candidate = sub.toString().replace('\\', '/');
                    if (!candidate.isEmpty()) {
                        return candidate;
                    }
                    break;
                }
            }
        }
        String filenameOnly = otherAbs.getFileName() != null
                ? otherAbs.getFileName().toString()
                : otherAbs.toString();
        return filenameOnly.replace('\\', '/');
    }

    private static String relativizeWhenWalksUp(Path baseAbs, Path otherAbs, String baseStr, String otherStr,
            String otherStrLc, String baseStrLc) {
        if (otherStrLc.startsWith(baseStrLc)) {
            String trimmed = otherStr.substring(baseStr.length());
            if (trimmed.startsWith("\\") || trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1);
            }
            String normalized = trimmed.replace('\\', '/');
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        for (int i = 0; i < otherAbs.getNameCount(); i++) {
            if (otherAbs.getName(i).toString().equalsIgnoreCase("src")) {
                Path sub = otherAbs.subpath(i, otherAbs.getNameCount());
                String candidate = sub.toString().replace('\\', '/');
                if (!candidate.isEmpty()) {
                    return candidate;
                }
                break;
            }
        }
        String leaf = otherAbs.getFileName() != null
                ? otherAbs.getFileName().toString()
                : otherAbs.toString();
        return leaf.replace('\\', '/');
    }

    private static String relativizeFallback(Path base, Path other) {
        try {
            Path baseAbs = base.toAbsolutePath().normalize();
            Path otherAbs = other.toAbsolutePath().normalize();
            String baseStr = baseAbs.toString();
            String otherStr = otherAbs.toString();
            if (otherStr.toLowerCase(Locale.ROOT).startsWith(baseStr.toLowerCase(Locale.ROOT))) {
                String trimmed = otherStr.substring(baseStr.length());
                if (trimmed.startsWith("\\") || trimmed.startsWith("/")) {
                    trimmed = trimmed.substring(1);
                }
                return trimmed.replace('\\', '/');
            }
            String leaf = otherAbs.getFileName() != null
                    ? otherAbs.getFileName().toString()
                    : otherAbs.toString();
            return leaf.replace('\\', '/');
        } catch (RuntimeException e) {
            return other.toString().replace('\\', '/');
        }
    }
}
