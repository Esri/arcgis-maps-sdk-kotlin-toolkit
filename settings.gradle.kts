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

include(
    ":template",
    ":composable-map",
    ":template-app",
    ":bom"
)

project(":template").projectDir = File(rootDir, "toolkit/template")
project(":composable-map").projectDir = File(rootDir, "toolkit/composable-map")
project(":template-app").projectDir = File(rootDir, "microapps/TemplateApp/app")
project(":bom").projectDir = File(rootDir, "bom")
