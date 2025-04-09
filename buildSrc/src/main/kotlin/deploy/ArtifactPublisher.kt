/*
 *
 *  Copyright 2023 Esri
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

package deploy

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import java.net.URI

/**
 * This class will provide maven publication to projects which apply it as a plugin.
 * This class does not support platform publication (e.g. a BOM artifact).
 *
 * Though the name is `ArtifactPublisher` applying this plugin will provide
 * all maven publication tasks to a project, such as publishToMavenLocal.
 * The name is qualified to not overload the term `Publisher`.
 *
 * @since 200.2.0
 */
class ArtifactPublisher : Plugin<Project> {
    override fun apply(project: Project) {
        val artifactoryGroupId: String by project
        val artifactoryArtifactBaseId: String by project
        val artifactoryUrl: String by project
        val artifactoryUsername: String by project
        val artifactoryPassword: String by project
        val versionNumber: String by project
        val finalBuild: Boolean = (project.properties["finalBuild"] ?: "false")
            .run { this == "true" }
        val buildNumber: String by project
        val artifactVersion: String = if (finalBuild) {
            versionNumber
        } else {
            "$versionNumber-$buildNumber"
        }
        val artifactoryArtifactId: String = "$artifactoryArtifactBaseId-${project.name}"
        
        project.pluginManager.apply(MavenPublishPlugin::class.java)
        project.afterEvaluate {
            project.extensions.configure<PublishingExtension> {
                repositories {
                    repositories.maven {
                        url = URI.create(artifactoryUrl)
                        credentials {
                            username = artifactoryUsername
                            password = artifactoryPassword
                        }
                    }
                }
                publications {
                    publications.create(
                        project.name,
                        MavenPublication::class.java
                    ) {
                        groupId = artifactoryGroupId
                        artifactId = artifactoryArtifactId
                        version = artifactVersion
                        
                        from(project.components["release"])
                    }
                }
            }

            tasks.findByName("publish${project.name.replaceFirstChar { it.uppercase() }}PublicationToMavenRepository")
                ?.dependsOn("assembleRelease")
            tasks.findByName("publishToMavenLocal")?.dependsOn("assembleRelease")
        }
    }
}
