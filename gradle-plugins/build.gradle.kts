repositories {
    google()
    mavenCentral()
}

plugins {
    `kotlin-dsl`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("artifactDeploy") {
            group = "internal"
            id = "artifact-deploy"
            version = "1.0"
            implementationClass = "com.arcgismaps.ArtifactPublisher"
        }
    }
}

dependencies {
    // Module-specific dependencies go here
}
