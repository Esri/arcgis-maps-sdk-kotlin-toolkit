import com.arcgismaps.SyncTestDataPluginExtension
import java.util.Properties

plugins {
    `kotlin-dsl`
    alias(libs.plugins.arcgismaps.testdata.sync)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("../local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

configure<SyncTestDataPluginExtension>{
    adbExe.set(File("${ localProperties.getProperty("sdk.dir") }/platform-tools/adb"))
    testApplicationId.set("")
    testDataSyncPath.set(rootProject.properties["testDataSyncPath"] as String)
    val kotlinDirPath = rootProject.properties["kotlinDirPath"] as String
    kotlinDir.set(File(project.rootDir, kotlinDirPath).canonicalFile)
}

tasks.named("grantDevicePermissions") {
    doFirst {
        error(
            "This task is disabled because :build-logic-internal is not an Android module and has no testApplicationId. " +
                    "Run a module-specific grantDevicePermissions task instead (e.g. :geoview-compose:grantDevicePermissions)."
        )
    }
}

gradlePlugin {
    plugins {
        create("androidIntegrationTesting") {
            id = "android-integration-testing"
            implementationClass = "AndroidIntegrationTestingConventionPlugin"
        }
    }
}

dependencies{
    compileOnly(libs.android.gradle.plugin)
    implementation(libs.arcgismaps.testdata.sync.gradle.plugin)
}
