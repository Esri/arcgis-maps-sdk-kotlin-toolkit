import com.esri.arcgismaps.kotlin.build_logic.extensions.ToolkitRegistryExtension
import com.esri.arcgismaps.kotlin.build_logic.extensions.api
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
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
        val versionNumber: String by project
        val buildNumber: String by project
        val finalBuild: Boolean = (project.properties["finalBuild"] ?: "false").toString() == "true"
        val artifactVersion = if (finalBuild) versionNumber else "$versionNumber-$buildNumber"
        val artifactoryArtifactId = "$artifactoryArtifactBaseId-${project.name}"

        // now find projects which are publishable based on their inclusion  of the toolkit plugin,
        // and add them as api dependencies.
        afterEvaluate {
            dependencies {
                constraints {
                    // grab the populated set
                    val registry = rootProject.extensions.getByType(ToolkitRegistryExtension::class.java)
                    registry.toolkitProjects.forEach { p ->
                        val moduleArtifactId = "$artifactoryArtifactBaseId-${p.name}"
                        // add the toolkit project as api
                        api("$artifactoryGroupId:$moduleArtifactId:$artifactVersion")
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
                    version = artifactVersion

                    from(components["javaPlatform"])
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
