/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.dokka) apply true
}

val versionNumber: String by project

// make this project get evaluated after all the other projects
// so that we can be sure the logic to determine released components
// below works
rootProject.subprojects.filter {
    it.name != project.name && it.name != "bom"
}.forEach {
    evaluationDependsOn(":${it.name}")
}

// only run kdoc on components which are released. Only modules that apply
// the `artifact-deploy` plugin are released.
// TODO: flag released modules directly.
val releasedModules = project.rootProject.subprojects.filter {
    it.plugins.findPlugin("artifact-deploy") != null
}

// determine the released toolkit components
val releasedSourceSetPaths = releasedModules.map { subproject ->
    // add all the intended library projects as sourceSets below
    File(rootDir, "toolkit/${subproject.name}/src/main/java").canonicalPath
}

tasks {
    //./gradlew :kdoc:dokkaHtml
    // doc output will be under `documentation/build/dokka/html`.
    dokkaHtml {
        pluginConfiguration<org.jetbrains.dokka.versioning.VersioningPlugin, org.jetbrains.dokka.versioning.VersioningConfiguration> {
            version = versionNumber
        }

        moduleName.set("arcgis-maps-kotlin-toolkit")
        dokkaSourceSets {
            named("main") {
                sourceRoots.from(releasedSourceSetPaths)
            }

            configureEach {
                platform.set(org.jetbrains.dokka.Platform.jvm)
                perPackageOption {
                    matchingRegex.set(".*internal.*")
                    suppress.set(true)
                }
                
                perPackageOption {
                    reportUndocumented.set(true)
                }
            }
        }
    }
}

android {
    namespace = "com.arcgismaps.toolkit.doc"
    compileSdk = libs.versions.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    // Puts the version in the KDoc
    dokkaPlugin(libs.dokka.versioning)
    // put exposed dependencies in dokka's classpath
    implementation(project(":mapsSdk"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
}

