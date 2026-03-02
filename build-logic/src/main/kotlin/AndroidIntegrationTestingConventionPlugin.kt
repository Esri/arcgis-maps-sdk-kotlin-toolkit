import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.arcgismaps.GrantDevicePermissions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import java.util.Locale

/**
 * Example convention plugin:
 * - applies/coordinates tasks needed before connectedAndroidTest tasks run
 * - keep it lazy and scoped to Android app modules
 */
class AndroidIntegrationTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.library") {
                val android = extensions.getByType(LibraryExtension::class.java)

                val syncTestDataBeforeInstrumentedTests: String by project

                tasks.register<GrantDevicePermissions>("grantDevicePermissions") {
                    adbExe.set(android.adbExecutable.absoluteFile)
                    testApplicationId.set(android.defaultConfig.testApplicationId)
                }

                afterEvaluate {

                    android.testVariants.forEach { testVariant ->
                        val capitalizedTestVariantName =
                            testVariant.name.replaceFirstChar {
                                if (it.isLowerCase()) {
                                    it.titlecase(Locale.US)
                                }
                                else {
                                    it.toString()
                                }
                            }
                        tasks.named("connected$capitalizedTestVariantName") {
                            if (syncTestDataBeforeInstrumentedTests.toBoolean()) {
                                // Uses adb to sync the test data to the test device.
                                dependsOn(gradle.includedBuild("build-logic").task(":syncTestData"))
                            }
                            // Grants storage permissions requested by the test app.
                            dependsOn("grantDevicePermissions")
                            // Deletes ic-output folder before running connectedAndroidTests
                            dependsOn(gradle.includedBuild("build-logic").task(":deleteICOutput"))
                        }

                        // Make sure the permissions task only runs after the install task, otherwise the app
                        // may not be installed yet.
                        tasks.named("grantDevicePermissions").dependsOn("install$capitalizedTestVariantName")
                    }
                }
            }
    } }
}
