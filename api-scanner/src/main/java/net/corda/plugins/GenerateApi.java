package net.corda.plugins;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.*;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static net.corda.plugins.ApiScanner.GROUP_NAME;

@SuppressWarnings("unused")
public class GenerateApi extends DefaultTask {

    private final File outputDir;
    private String baseName;
    private String version;

    public GenerateApi() {
        setGroup(GROUP_NAME);
        setDescription("Aggregates API scan results found in any sub-projects into a single output.");
        outputDir = new File(getProject().getBuildDir(), "api");
        baseName = "api-" + getProject().getName();
        version = getProject().getVersion().toString();
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Input
    public String getBaseName() {
        return baseName;
    }

    @Input
    public String getVersion() {
        return version;
    }

    @InputFiles
    public FileCollection getSources() {
        // This will trigger configuration of every ScanApi task in the project.
        return getProject().files(getProject().getAllprojects().stream()
            .flatMap(project -> project.getTasks()
                         .withType(ScanApi.class)
                         .matching(ScanApi::isEnabled)
                         .stream())
            .flatMap(scanTask -> scanTask.getTargets().getFiles().stream())
            .sorted(comparing(File::getName))
            .collect(toList())
        );
    }

    private StringBuilder appendVersion(@Nonnull StringBuilder builder) {
        if (!version.isEmpty()) {
            builder.append('-').append(version);
        }
        return builder;
    }

    @OutputFile
    public File getTarget() {
        String fileName = appendVersion(new StringBuilder(baseName)).append(".txt").toString();
        return new File(outputDir, fileName);
    }

    @TaskAction
    public void generate() {
        FileCollection apiFiles = getSources();
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(getTarget()))) {
            for (File apiFile : apiFiles) {
                Files.copy(apiFile.toPath(), output);
            }
        } catch (IOException e) {
            getLogger().error("Failed to generate API file: {}", e.getMessage());
            throw new InvalidUserCodeException(e.getMessage(), e);
        }
    }
}
