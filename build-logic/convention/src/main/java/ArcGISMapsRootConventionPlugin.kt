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
