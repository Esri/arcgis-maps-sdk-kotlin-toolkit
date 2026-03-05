plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("androidIntegrationTesting") {
            id = "android-integration-testing"
            implementationClass = "DummyPlugin"
        }
    }
}
