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
    }
}
include(
    ":template",
    ":template-app",
    ":bom"
)

project(":template").projectDir = File(rootDir, "toolkit/template")
project(":template-app").projectDir = File(rootDir, "microapps/TemplateApp/app")
project(":bom").projectDir = File(rootDir, "bom")
