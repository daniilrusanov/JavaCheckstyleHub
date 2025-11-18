package com.checkstylehub.analyzer.service;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for running Checkstyle analysis on Java files.
 * Handles configuration loading and violation collection.
 */
@Service
public class CheckstyleService {

    private final CheckstyleConfigurationService configurationService;

    public CheckstyleService(CheckstyleConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * Finds all .java files in the given directory recursively.
     *
     * @param startDir the root directory to search
     * @return list of Java file paths
     * @throws IOException if directory traversal fails
     */
    public List<Path> findJavaFiles(Path startDir) throws IOException {
        try (Stream<Path> stream = Files.walk(startDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Runs Checkstyle analysis on the provided Java files.
     *
     * @param baseDir         the base directory of the project
     * @param javaFiles       list of Java files to analyze
     * @param customConfigXml optional custom XML configuration
     * @return list of violations (AuditEvents)
     * @throws CheckstyleException if analysis fails
     */
    public List<AuditEvent> runCheckstyle(Path baseDir, List<Path> javaFiles, String customConfigXml)
            throws CheckstyleException {

        final List<AuditEvent> violations = new ArrayList<>();

        try {
            AuditListener listener = new AuditListener() {
                @Override
                public void auditStarted(AuditEvent event) {
                }

                @Override
                public void auditFinished(AuditEvent event) {
                }

                @Override
                public void fileStarted(AuditEvent event) {
                }

                @Override
                public void fileFinished(AuditEvent event) {
                }

                @Override
                public void addError(AuditEvent event) {
                    if (event.getSeverityLevel() == SeverityLevel.ERROR ||
                            event.getSeverityLevel() == SeverityLevel.WARNING) {
                        violations.add(event);
                    }
                }

                @Override
                public void addException(AuditEvent event, Throwable throwable) {
                    System.err.println("Checkstyle exception on file " + event.getFileName() + ": " + throwable.getMessage());
                }
            };

            org.xml.sax.InputSource configSource = loadConfiguration(customConfigXml);

            com.puppycrawl.tools.checkstyle.api.Configuration config =
                    ConfigurationLoader.loadConfiguration(
                            configSource,
                            new PropertiesExpander(System.getProperties()),
                            ConfigurationLoader.IgnoredModulesOptions.OMIT
                    );

            Checker checker = new Checker();
            checker.setModuleClassLoader(Checker.class.getClassLoader());
            checker.configure(config);
            checker.addListener(listener);
            checker.setBasedir(baseDir.toAbsolutePath().toString());

            List<java.io.File> filesToProcess = javaFiles.stream()
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            checker.process(filesToProcess);
            checker.destroy();

            return violations;

        } catch (Exception e) {
            throw new CheckstyleException("Failed to run Checkstyle analysis: " + e.getMessage(), e);
        }
    }

    /**
     * Loads Checkstyle XML configuration from custom input or active database configuration.
     *
     * @param customConfigXml optional custom configuration XML
     * @return InputSource containing the configuration
     * @throws IOException if configuration loading fails
     */
    private InputSource loadConfiguration(String customConfigXml) throws IOException {
        if (customConfigXml != null && !customConfigXml.isBlank()) {
            return new InputSource(new ByteArrayInputStream(customConfigXml.getBytes()));
        } else {
            String activeConfigXml = configurationService.getActiveConfigurationXml();
            return new InputSource(new ByteArrayInputStream(activeConfigXml.getBytes()));
        }
    }
}
