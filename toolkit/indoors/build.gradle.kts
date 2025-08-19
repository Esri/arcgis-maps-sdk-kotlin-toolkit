plugins {
    alias(libs.plugins.arcgismaps.kotlin.toolkit)
    alias(libs.plugins.artifact.deploy)
}

android {
    namespace = "com.arcgismaps.toolkit.indoors"
}

apiValidation {
    ignoredClasses.add("com.arcgismaps.toolkit.featureforms.BuildConfig")
    // todo: remove when this is resolved https://github.com/Kotlin/binary-compatibility-validator/issues/74
    // compose compiler generates public singletons for internal compose functions. this may be resolved in the compose
    // compiler.
    val composableSingletons = listOf(
        "com.arcgismaps.toolkit.indoors.ComposableSingletons\$SiteAndFacilitySelectorKt"
    )

    ignoredClasses.addAll(composableSingletons)
}

dependencies {
    // Module-specific dependencies go here
    androidTestImplementation(project(":composable-map"))
}
