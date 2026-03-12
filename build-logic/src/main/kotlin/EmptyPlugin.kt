import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * An empty plugin that does nothing.
 *
 * @since 300.0.0
 */
class EmptyPlugin : Plugin<Project> {
    override fun apply(target: Project) {}
}
