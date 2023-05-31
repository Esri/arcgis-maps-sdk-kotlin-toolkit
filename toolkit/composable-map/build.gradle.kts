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
val versionNumber: String by project
val buildNumber: String by project
val artifactVersion: String = "$versionNumber-$buildNumber"

android {
    namespace = "com.arcgismaps.toolkit.composablemap"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

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
        jvmTarget = "1.8"
    }
    @Suppress("UnstableApiUsage")
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
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
    implementation(libs.arcgis)
    implementation(libs.bundles.composeCore)
    implementation(libs.bundles.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(libs.bundles.composeTest)
    debugImplementation(libs.bundles.debug)
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
