import org.gradle.configurationcache.extensions.capitalized

val projects = listOf("template", "compass")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
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
