plugins {
    `kotlin-dsl`
}

// Since the build-logic-internal build is not included for non-internal developers,
// plugins created there are not accessible to them. That means modules that
// apply plugins from build-logic-internal (e.g., `android-integration-testing`)
// would fail to compile.
//
// To avoid this, we register a placeholder plugin with the same ID in the
// publicly accessible build-logic build. This lets non-internal developers
// compile and work with the code, while internal developers get the real
// implementation from build-logic-internal.
gradlePlugin {
    plugins {
        create("androidIntegrationTesting") {
            id = "android-integration-testing"
            implementationClass = "EmptyPlugin"
        }
    }
}
