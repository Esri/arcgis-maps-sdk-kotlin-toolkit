import org.gradle.api.Plugin
import org.gradle.api.Project

class DummyPlugin : Plugin<Project> {
    override fun apply(target: Project) {}
}
