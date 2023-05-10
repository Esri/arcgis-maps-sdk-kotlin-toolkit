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
        maven(url = "https://esri.jfrog.io/artifactory/arcgis")
        maven(url = "https://olympus.esri.com/artifactory/arcgisruntime-repo/")
    }
}
include(
    ":template",
    ":template-app",
    ":example-app",
    ":bom"
)

project(":template").projectDir = File(rootDir, "toolkit/template")
project(":template-app").projectDir = File(rootDir, "microapps/TemplateApp/app")
project(":example-app").projectDir = File(rootDir, "microapps/ExampleApp/app")
project(":bom").projectDir = File(rootDir, "bom")
