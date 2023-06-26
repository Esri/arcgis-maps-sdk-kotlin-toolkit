import org.gradle.configurationcache.extensions.capitalized

val projects = listOf("template", "featureforms", "authentication", "compass")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// The version of the ArcGIS Maps SDK for Kotlin dependency
val sdkVersionNumber: String by settings
// The build number of the ArcGIS Maps SDK for Kotlin dependency
val sdkBuildNumber: String by settings

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven {
            url = java.net.URI(
                "https://olympus.esri.com/artifactory/arcgisruntime-repo/"
            )
        }
    }
    
    versionCatalogs {
        create("arcgis") {
            version("mapsSdk", "$sdkVersionNumber-$sdkBuildNumber")
            library("mapsSdk", "com.esri", "arcgis-maps-kotlin").versionRef("mapsSdk")
        }
    }
}

var includedProjects = projects.flatMap { listOf(":$it", ":$it-app") }.toTypedArray()
include(*includedProjects)
include (":bom")
include (":composable-map")

projects.forEach {
    project(":$it").projectDir = File(rootDir, "toolkit/$it")
    project(":$it-app").projectDir = File(rootDir, "microapps/${it.capitalized()}App/app")
}

project(":bom").projectDir = File(rootDir, "bom")
project(":composable-map").projectDir = File(rootDir, "toolkit/composable-map")
