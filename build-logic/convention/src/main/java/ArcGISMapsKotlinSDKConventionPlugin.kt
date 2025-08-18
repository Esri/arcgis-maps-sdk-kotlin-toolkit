import com.esri.arcgismaps.kotlin.build_logic.convention.ArcGISMapsKotlinSDKDependency
import org.gradle.api.Plugin
import org.gradle.api.Project

class ArcGISMapsKotlinSDKConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Configure the ArcGIS Maps SDK dependency
            ArcGISMapsKotlinSDKDependency.configureArcGISMapsDependencies(target)
        }
    }
}
