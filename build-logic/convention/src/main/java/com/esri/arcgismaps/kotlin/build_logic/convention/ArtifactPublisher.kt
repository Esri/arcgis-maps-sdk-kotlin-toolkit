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

package com.esri.arcgismaps.kotlin.build_logic.convention

import com.esri.arcgismaps.kotlin.build_logic.registry.toolkitRegistryServiceProvider
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.configure
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
object ArtifactPublisher {
    fun configureArtifactPublisher(project: Project) {
        val artifactoryGroupId: String by project
        val artifactoryArtifactBaseId: String by project
        val artifactoryUrl: String by project
        val artifactoryUsername: String by project
        val artifactoryPassword: String by project

        // Use centralized version provider, publish using internal `buildnum.txt` as source
        val artifactVersionProvider = VersionProvider.artifactVersionProvider(project, true)
        // Built the artifact id for the given project
        val artifactoryArtifactId = "$artifactoryArtifactBaseId-${project.name}"

        project.pluginManager.apply(MavenPublishPlugin::class.java)
        project.afterEvaluate {
            // Check if this module is releasable
            val registryService = toolkitRegistryServiceProvider(this).get()
            val isReleasable = registryService.isModuleReleasable(this).get()
            if (!isReleasable) return@afterEvaluate

            extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        url = URI.create(artifactoryUrl)
                        credentials {
                            username = artifactoryUsername
                            password = artifactoryPassword
                        }
                    }
                }
                publications {
                    create(name, MavenPublication::class.java) {
                        groupId = artifactoryGroupId
                        artifactId = artifactoryArtifactId
                        version = artifactVersionProvider.get()

                        from(components.getByName("release"))
                    }
                }
            }

            tasks.findByName("publish${name.replaceFirstChar { it.uppercase() }}PublicationToMavenRepository")
                ?.dependsOn("assembleRelease")
            tasks.findByName("publishToMavenLocal")?.dependsOn("assembleRelease")
        }
    }
}
