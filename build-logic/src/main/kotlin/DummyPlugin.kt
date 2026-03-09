import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A dummy plugin that does nothing.
 *
 * @since 300.0.0
 */
class DummyPlugin : Plugin<Project> {
    override fun apply(target: Project) {}
}
