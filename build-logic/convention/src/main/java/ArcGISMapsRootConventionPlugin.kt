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

import com.esri.arcgismaps.kotlin.build_logic.registry.ToolkitRegistryService
import org.gradle.api.Plugin
import org.gradle.api.Project as GradleProject

class ArcGISMapsRootConventionPlugin : Plugin<GradleProject> {
    override fun apply(target: GradleProject) {
        if (target != target.rootProject) {
            throw IllegalStateException("Plugin must only be applied to the root project ")
        }
        with(target) {
            // Initial registration of the ToolkitRegistryService, Gradle creates the provider here,
            // which will be shall be used as the centralized Toolkit configuration.
            target.gradle.sharedServices.registerIfAbsent(
                /* name = */ ToolkitRegistryService.NAME,
                /* implementationType = */ ToolkitRegistryService::class.java
            )
        }
    }
}
