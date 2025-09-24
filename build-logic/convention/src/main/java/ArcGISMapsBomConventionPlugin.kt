import com.esri.arcgismaps.kotlin.build_logic.convention.VersionProvider
import com.esri.arcgismaps.kotlin.build_logic.registry.ToolkitRegistry
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate

class ArcGISMapsBomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Platform  publishing
        pluginManager.apply("maven-publish")
        pluginManager.apply("java-platform")

        // Find these in properties passed through command line or read from GRADLE_HOME/gradle.properties
        // or local gradle.properties
        val artifactoryGroupId: String by project
        val artifactoryArtifactBaseId: String by project
        val artifactoryUrl: String by project
        val artifactoryUsername: String by project
        val artifactoryPassword: String by project

        // Use centralized version provider
        val versionConfig = VersionProvider.getVersionConfig(project)
        val artifactVersion = versionConfig.map { it.artifactVersion }
        val artifactoryArtifactId = "$artifactoryArtifactBaseId-${project.name}"

        // Use toolkit registry to find projects which are releasable
        // Wait until all projects are evaluated before discovering modules
        gradle.projectsEvaluated {
            dependencies {
                constraints {
                    // Get releasable modules and version after all projects are evaluated
                    val releasableModules = ToolkitRegistry.getReleasableModules(rootProject).get()
                    val resolvedVersion = artifactVersion.get()
                    releasableModules.forEach { moduleConfig ->
                        val moduleArtifactId = "$artifactoryArtifactBaseId-${moduleConfig.name}"
                        val dependency = "$artifactoryGroupId:$moduleArtifactId:$resolvedVersion"
                        // add the toolkit project as api
                        add("api", dependency)
                    }
                }
            }
        }

        /**
         * Maven publication configuration for aar and pom file. Run as follows:
         * ./gradlew publishAarPublicationToMavenRepository -PartifactoryUsername=<username> -PartifactoryPassword=<password>
         *
         * More details:
         * https://docs.gradle.org/current/userguide/publishing_maven.html
         */
        extensions.configure(PublishingExtension::class.java) {
            publications {
                create("bom", MavenPublication::class.java) {
                    groupId = artifactoryGroupId
                    artifactId = artifactoryArtifactId
                    version = artifactVersion.get()

                    from(components.getByName("javaPlatform"))
                }
            }
            repositories {
                maven {
                    url = uri(artifactoryUrl)
                    credentials {
                        username = artifactoryUsername
                        password = artifactoryPassword
                    }
                }
            }
        }
    }
}
