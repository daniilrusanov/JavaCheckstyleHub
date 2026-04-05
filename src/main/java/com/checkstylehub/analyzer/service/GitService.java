package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.exception.RepositoryAccessException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Git repository operations.
 * Handles cloning repositories and cleaning up temporary directories.
 */
@Service
public class GitService {

    /**
     * Resolves the latest commit hash for the remote HEAD (default branch) without cloning.
     * <p>
     * Note: {@code setHeads(true)} in JGit lists only {@code refs/heads/*} and omits the symbolic
     * {@code HEAD} ref on many servers — use a full {@code ls-remote} and fall back to common
     * default branch names when needed.
     */
    public String getLatestCommitHash(String repoUrl) {
        try {
            Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(repoUrl)
                    .call();
            Map<String, Ref> byName = new HashMap<>();
            for (Ref r : refs) {
                byName.put(r.getName(), r);
            }
            Ref head = byName.get("HEAD");
            if (head == null) {
                head = pickDefaultBranchRef(byName);
            }
            if (head == null) {
                throw new RepositoryAccessException("Remote has no HEAD or branches");
            }
            Ref resolved = head;
            while (resolved.isSymbolic()) {
                String targetRefName = resolved.getTarget().getName();
                resolved = byName.get(targetRefName);
                if (resolved == null) {
                    throw new RepositoryAccessException("Could not resolve symbolic HEAD to " + targetRefName);
                }
            }
            if (resolved.getObjectId() == null) {
                throw new RepositoryAccessException("HEAD ref has no object id");
            }
            return resolved.getObjectId().getName();
        } catch (GitAPIException e) {
            throw new RepositoryAccessException("Repository access error: " + e.getMessage(), e);
        }
    }

    /**
     * When {@code HEAD} is absent from ls-remote, use the usual default branch candidates, else any branch.
     */
    private Ref pickDefaultBranchRef(Map<String, Ref> byName) {
        List<String> preferred = List.of(
                "refs/heads/main",
                "refs/heads/master",
                "refs/heads/trunk",
                "refs/heads/develop"
        );
        for (String name : preferred) {
            Ref r = byName.get(name);
            if (r != null && r.getObjectId() != null) {
                return r;
            }
        }
        List<String> branchNames = new ArrayList<>();
        for (String name : byName.keySet()) {
            if (name.startsWith("refs/heads/")) {
                branchNames.add(name);
            }
        }
        branchNames.sort(String::compareTo);
        for (String name : branchNames) {
            Ref r = byName.get(name);
            if (r != null && r.getObjectId() != null) {
                return r;
            }
        }
        return null;
    }

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
