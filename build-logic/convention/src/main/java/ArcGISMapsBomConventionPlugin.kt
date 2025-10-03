/*
 *
 *  Copyright 2025 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import com.esri.arcgismaps.kotlin.build_logic.convention.VersionProvider
import com.esri.arcgismaps.kotlin.build_logic.registry.toolkitRegistryServiceProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate

class ArcGISMapsBomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            // Platform  publishing
            apply("maven-publish")
            apply("java-platform")
        }

        // Find these in properties passed through command line
        // or read from GRADLE_HOME/gradle.properties or local gradle.properties
        val artifactoryGroupId: String by project
        val artifactoryArtifactBaseId: String by project
        val artifactoryUrl: String by project
        val artifactoryUsername: String by project
        val artifactoryPassword: String by project

        // Use centralized version provider, publish using internal `buildnum.txt` as source
        val artifactVersionProvider = VersionProvider.artifactVersionProvider(project, true)
        val artifactoryArtifactId = "$artifactoryArtifactBaseId-${project.name}"

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
                    version = artifactVersionProvider.get()

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

        // Get the toolkit registry provider
        val registryServiceProvider = toolkitRegistryServiceProvider(this)
        // Lazily get releasable modules from the service provider
        val releasableModulesProvider = registryServiceProvider.map { service ->
            service.toolkitModules.get().filter { it.releasable }
        }

        // Use toolkit registry to find projects which are releasable
        // Wait until all projects are evaluated before discovering modules
        gradle.projectsEvaluated {
            // Get releasable modules and version after all projects are evaluated
            val releasableModules = releasableModulesProvider.get()
            val artifactVersion = artifactVersionProvider.get()
            dependencies {
                constraints {
                    releasableModules.forEach { moduleConfig ->
                        val moduleArtifactId = "$artifactoryArtifactBaseId-${moduleConfig.name}"
                        val dependency = "$artifactoryGroupId:$moduleArtifactId:$artifactVersion"
                        // Add the toolkit project as api dependency
                        add("api", dependency)
                    }
                }
            }
        }
    }
}
