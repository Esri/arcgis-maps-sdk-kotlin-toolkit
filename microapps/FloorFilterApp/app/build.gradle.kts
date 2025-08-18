plugins {
    alias(libs.plugins.arcgismaps.kotlin.microapp)
}

android {
    namespace = "com.arcgismaps.toolkit.floorfilterapp"
    
    defaultConfig {
        applicationId = "com.arcgismaps.toolkit.floorfilterapp"
    }
}

dependencies {
    // Module-specific dependencies go here
    implementation(project(":indoors"))
    implementation(project(":geoview-compose"))
}
