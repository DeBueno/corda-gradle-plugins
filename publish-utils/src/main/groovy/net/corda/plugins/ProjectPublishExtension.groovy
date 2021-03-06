package net.corda.plugins

import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

class ProjectPublishExtension {
    private final PublishTasks task
    private final MavenDependencyExtension dependencyConfig

    @Inject
    ProjectPublishExtension(ObjectFactory objects, PublishTasks task) {
        this.task = task
        this.dependencyConfig = objects.newInstance(MavenDependencyExtension)
    }

    /**
     * Use a different name from the current project name for publishing.
     * Set this after all other settings that need to be configured
     */
    void name(String name) {
        task.setPublishName(name)
    }

    /**
     * Get the publishing name for this project.
     */
    String name() {
        return task.getPublishName()
    }

    /**
     * True when we do not want to publish default Java components
     */
    Boolean disableDefaultJar = false

    /**
     * True if publishing a WAR instead of a JAR. Forces disableDefaultJAR to "true" when true
     */
    Boolean publishWar = false

    /**
     * True if publishing sources to remote repositories
     */
    Boolean publishSources = true

    /**
     * True if publishing javadoc to remote repositories
     */
    Boolean publishJavadoc = true

    /**
     * @return default configuration for Maven dependencies generated by "dependenciesFrom" field.
     */
    MavenDependencyExtension getDependencyConfig() {
        return dependencyConfig
    }

    /**
     * The Gradle configuration that defines this artifact's dependencies.
     * This overrides the dependencies that would otherwise be derived
     * from "components.java" or "components.web".
     * Implies both "disableDefaultJar=true" and "publishWar=false"
     *
     * <pre>
     * {@code
     * publish {
     *     dependenciesFrom configurations.runtimeArtifacts
     * }
     * }
     * </pre>
     * or
     * <pre>
     * {@code
     * publish {
     *     dependenciesFrom(configurations.runtimeArtifacts) {
     *         defaultScope = 'compile'
     *     }
     * }
     * }
     * </pre>
     *
     * @param dependencies
     * @param configuration
     */
    void dependenciesFrom(Configuration dependencies, Closure configuration = null) {
        task.publishDependencies = dependencies
        ConfigureUtil.configure(configuration, dependencyConfig)
    }
}