import com.android.build.api.dsl.ApplicationExtension
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

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("android-application").get().get().pluginId)
                apply(libs.findPlugin("kotlin-android").get().get().pluginId)
                apply(libs.findPlugin("compose-compiler").get().get().pluginId)
            }
            val extension = extensions.getByType<ApplicationExtension>()
            configureKotlinAndroid(extension)
            configureAndroidCompose(extension)
            
            // Add common Compose dependencies for application modules
            dependencies {
                implementation(platform(libs.findLibrary("androidx-compose-bom").get()))
                implementation(libs.findBundle("composeCore").get())
                implementation(libs.findBundle("core").get())
                implementation(libs.findLibrary("androidx-activity-compose").get())
                implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                
                testImplementation(libs.findBundle("unitTest").get())
                androidTestImplementation(platform(libs.findLibrary("androidx-compose-bom").get()))
                androidTestImplementation(libs.findBundle("composeTest").get())
                debugImplementation(libs.findBundle("debug").get())
            }
        }
    }
}
