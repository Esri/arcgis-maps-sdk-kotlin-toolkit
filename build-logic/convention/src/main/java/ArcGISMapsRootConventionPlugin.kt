import com.esri.arcgismaps.kotlin.build_logic.extensions.ToolkitRegistryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class ArcGISMapsRootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "The ArcGISMapsRootConventionPlugin must be applied to the root project only."
        }
        // Create the single registry extension on the root project
        target.extensions.create<ToolkitRegistryExtension>("toolkitRegistry")
    }
}
