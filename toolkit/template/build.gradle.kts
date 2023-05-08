plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

// Find these in properties passed through command line or read from GRADLE_HOME/gradle.properties
// or local gradle.properties
val artifactoryGroupId: String by project
val artifactoryArtifactBaseId: String by project
val artifactoryArtifactId: String = "$artifactoryArtifactBaseId-${project.name}"
val artifactoryUrl: String by project
val artifactoryUsername: String by project
val artifactoryPassword: String by project
// TODO: figure out versioning
val artifactVersion: String = "xx.xx"

android {
    namespace = "com.arcgismaps.toolkit.template"
    compileSdk = 33

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    @Suppress("UnstableApiUsage")
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }
    // If this were not an android project, we would just write `explicitApi()` in the Kotlin scope.
    // but as an android project could write `freeCompilerArgs = listOf("-Xexplicit-api=strict")`
    // in the kotlinOptions above, but that would enforce api rules on the test code, which we don't want.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        if ("Test" !in name) {
            kotlinOptions.freeCompilerArgs += "-Xexplicit-api=strict"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation (platform("androidx.compose:compose-bom:2022.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2022.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


afterEvaluate {
    
    /**
     * Maven publication configuration for aar and pom file. Run as follows:
     * ./gradlew publishAarPublicationToMavenRepository -PartifactoryUsername=<username> -PartifactoryPassword=<password>
     *
     * More details:
     * https://docs.gradle.org/current/userguide/publishing_maven.html
     */
    publishing {
        publications {
            create<MavenPublication>("aar") {
                groupId = artifactoryGroupId
                artifactId = artifactoryArtifactId
                version = artifactVersion
                
                from(components["release"])
                
            }
        }
        
        repositories {
            maven {
                url = uri(artifactoryUrl)
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
            }
        }
    }
}
