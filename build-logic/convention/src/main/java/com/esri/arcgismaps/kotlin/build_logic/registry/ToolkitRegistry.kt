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

package com.esri.arcgismaps.kotlin.build_logic.registry

import com.esri.arcgismaps.kotlin.build_logic.extensions.ToolkitModuleExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Dynamic registry that discovers toolkit modules at runtime by scanning all projects
 * for the toolkit convention plugin and their releasable configuration.
 */
object ToolkitRegistry {

    /**
     * Configuration for a discovered toolkit module
     */
    data class ModuleConfig(
        val project: Project,
        val name: String,
        val releasable: Boolean
    ) {

        /**
         * Returns the source root path for documentation
         */
        val sourceRoot: String get() = "toolkit/$name/src/main/java"
    }

    /**
     * Discovers all projects that apply the toolkit convention plugin and builds
     * a provider that resolves their configuration when needed.
     */
    fun discoverToolkitModules(rootProject: Project): Provider<List<ModuleConfig>> {
        return rootProject.providers.provider {
            val toolkitModules = mutableListOf<ModuleConfig>()
            // Scan all subprojects for toolkit plugin
            rootProject.allprojects.forEach { project ->
                if (project.plugins.hasPlugin("arcgismaps.kotlin.toolkit")) {
                    project.extensions.findByType(ToolkitModuleExtension::class.java)
                        ?.let { toolkitModuleExt ->
                            toolkitModules.add(
                                ModuleConfig(
                                    project = project,
                                    name = project.name,
                                    releasable = toolkitModuleExt.releasable.getOrElse(true)
                                )
                            )
                        }
                }
            }
            toolkitModules.sortedBy { it.name }
        }
    }

    /**
     * Gets only releasable toolkit modules
     */
    fun getReleasableModules(rootProject: Project): Provider<List<ModuleConfig>> {
        return discoverToolkitModules(rootProject).map { modules ->
            modules.filter { it.releasable }
        }
    }

    /**
     * Checks if a specific module is releasable
     */
    fun isModuleReleasable(toolkitModule: Project): Provider<Boolean> {
        return discoverToolkitModules(toolkitModule.rootProject).map { modules ->
            modules.find { it.name == toolkitModule.name }?.releasable ?: false
        }
    }

    /**
     * Creates artifact dependencies configuration for BOM
     */
    fun createBomDependencies(
        rootProject: Project,
        artifactoryGroupId: String,
        artifactoryArtifactBaseId: String,
        artifactVersion: String
    ): Provider<List<String>> {
        return getReleasableModules(rootProject).map { modules ->
            modules.map { moduleConfig ->
                val moduleArtifactId = "$artifactoryArtifactBaseId-${moduleConfig.name}"
                "$artifactoryGroupId:$moduleArtifactId:$artifactVersion"
            }
        }
    }
}