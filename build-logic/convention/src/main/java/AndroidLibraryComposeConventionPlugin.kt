import com.android.build.api.dsl.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.configureAndroidCompose
import com.esri.arcgismaps.kotlin.build_logic.convention.configureKotlinAndroid
import com.esri.arcgismaps.kotlin.build_logic.convention.implementation
import com.esri.arcgismaps.kotlin.build_logic.convention.libs
import com.esri.arcgismaps.kotlin.build_logic.convention.testImplementation
import com.esri.arcgismaps.kotlin.build_logic.convention.androidTestImplementation
import com.esri.arcgismaps.kotlin.build_logic.convention.debugImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("android-library").get().get().pluginId)
                apply(libs.findPlugin("kotlin-android").get().get().pluginId)
                apply(libs.findPlugin("compose-compiler").get().get().pluginId)
                apply(libs.findPlugin("kotlin-parcelize").get().get().pluginId)
                apply(libs.findPlugin("kotlin-serialization").get().get().pluginId)
            }
            val extension = extensions.getByType<LibraryExtension>()
            configureKotlinAndroid(extension)
            configureAndroidCompose(extension)
            
            // Add common Compose dependencies for library modules
            dependencies {
                implementation(platform(libs.findLibrary("androidx-compose-bom").get()))
                implementation(libs.findBundle("composeCore").get())
                implementation(libs.findBundle("core").get())
                implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                implementation(libs.findLibrary("androidx-activity-compose").get())
                implementation(libs.findLibrary("androidx-material-icons").get())

                implementation(libs.findLibrary("kotlinx-serialization-json").get())
                
                testImplementation(libs.findBundle("unitTest").get())
                androidTestImplementation(libs.findBundle("composeTest").get())
                debugImplementation(libs.findBundle("debug").get())
            }
        }
    }
}
