// load artifactory information from the command line or gradle.properties

pluginManagement {
    repositories {
        val localProperties = java.util.Properties().apply {
            val localPropertiesFile = file("../local.properties")
            if (localPropertiesFile.exists()) {
                load(localPropertiesFile.inputStream())
            }
        }

        val artifactoryUrl: String =
            providers.gradleProperty("artifactoryUrl").orNull
                ?: localProperties.getProperty("artifactoryUrl")
                ?: ""

        val artifactoryUsername: String =
            providers.gradleProperty("artifactoryUsername").orNull
                ?: localProperties.getProperty("artifactoryUsername")
                ?: ""

        val artifactoryPassword: String =
            providers.gradleProperty("artifactoryPassword").orNull
                ?: localProperties.getProperty("artifactoryPassword")
                ?: ""

        if (!artifactoryUrl.isBlank()) {
            maven {
                url = java.net.URI(artifactoryUrl)
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
            }
        }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

val localProperties = java.util.Properties().apply {
    val localPropertiesFile = file("../local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val artifactoryUrl: String =
    providers.gradleProperty("artifactoryUrl").orNull
        ?: localProperties.getProperty("artifactoryUrl")
        ?: ""

val artifactoryUsername: String =
    providers.gradleProperty("artifactoryUsername").orNull
        ?: localProperties.getProperty("artifactoryUsername")
        ?: ""

val artifactoryPassword: String =
    providers.gradleProperty("artifactoryPassword").orNull
        ?: localProperties.getProperty("artifactoryPassword")
        ?: ""

dependencyResolutionManagement {
    repositories {
        if (!artifactoryUrl.isBlank()) {
            maven {
                url = java.net.URI(artifactoryUrl)
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic-internal"
