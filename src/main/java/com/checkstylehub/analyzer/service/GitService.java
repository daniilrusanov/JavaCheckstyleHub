package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.exception.RepositoryAccessException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Service for Git repository operations.
 * Handles cloning repositories and cleaning up temporary directories.
 */
@Service
public class GitService {

    /**
     * Clones a Git repository to a temporary directory using shallow clone (depth=1).
     *
     * @param repoUrl the repository URL
     * @return path to the cloned repository
     * @throws RepositoryAccessException if the repository is private, doesn't exist, or Git operation fails
     * @throws InterruptedException      if the operation is interrupted
     */
    public Path cloneRepository(String repoUrl) throws InterruptedException {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("repo_clone_");
        } catch (IOException e) {
            throw new RepositoryAccessException("Failed to create temporary directory", e);
        }

        try {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile())
                    .setDepth(1)
                    .setNoTags()
                    .call();
            return tempDir;
        } catch (GitAPIException e) {
            deleteTempDirectory(tempDir);
            throw new RepositoryAccessException("Repository access error: " + e.getMessage(), e);
        }
    }

    /**
     * Recursively deletes a temporary directory and all its contents.
     *
     * @param directory the directory path to delete
     */
    public void deleteTempDirectory(Path directory) {
        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        } catch (IOException e) {
            System.err.println("Failed to delete temporary directory: " + directory);
        }
    }
}
