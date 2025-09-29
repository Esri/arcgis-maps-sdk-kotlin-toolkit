package com.esri.arcgismaps.kotlin.build_logic.registry

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry

/**
 * A shared Gradle BuildService to act as a central registry for toolkit modules.
 */
abstract class ToolkitRegistryService : BuildService<BuildServiceParameters.None> {
    // A ListProperty to hold the configurations as they are registered.
    abstract val toolkitModules: ListProperty<ModuleConfig>

    fun isModuleReleasable(toolkitModule: Project): Provider<Boolean> {
        return toolkitModules.map { modules ->
            modules.find { it.name == toolkitModule.name }?.releasable ?: false
        }
    }

    companion object {
        const val NAME = "toolkitRegistry"
    }
}

/**
 * Configuration for a toolkit module.
 */
data class ModuleConfig(
    val path: String,
    val name: String,
    val releasable: Boolean
) {
    /**
     * Returns the source root path for documentation.
     */
    fun getSourceRoot(rootProject: Project): String =
        rootProject.project(path).projectDir.resolve("src/main/java").path
}

/**
 *
 * Obtain a provider for the shared build service. [BuildServiceRegistry.registerIfAbsent] ensures
 * only one instance of the service is created for the entire build.
 */
fun getToolkitRegistryServiceProvider(target: Project): Provider<ToolkitRegistryService> {
    return target.gradle.sharedServices.registerIfAbsent(
        /* name = */ ToolkitRegistryService.NAME,
        /* implementationType = */ ToolkitRegistryService::class.java
    )
}
