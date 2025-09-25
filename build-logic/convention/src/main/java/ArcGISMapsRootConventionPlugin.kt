import org.gradle.api.Plugin
import org.gradle.api.Project as GradleProject

class ArcGISMapsRootConventionPlugin : Plugin<GradleProject> {
    override fun apply(target: GradleProject) {
        with(target) {
            gradle.projectsEvaluated {
                // configureAndroidTestAggregation(target)
            }
        }
    }
}
