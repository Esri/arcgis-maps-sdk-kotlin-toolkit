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
    alias(libs.plugins.dokka)
}

val versionNumber: String by project
val buildNumber: String by project
val ignoreBuildNumber: String by project
val artifactVersion: String = if (ignoreBuildNumber == "true") {
    versionNumber
} else {
    "$versionNumber-$buildNumber"
}

// make this project get evaluated after all the other projects
// so that we can be sure the logic to determine released components
// below works
rootProject.subprojects.filter {
    it.name != project.name && it.name != "bom"
}.forEach {
    evaluationDependsOn(":${it.name}")
}

val releasedModules = project.rootProject.subprojects.filter {
    it.plugins.findPlugin("artifact-deploy") != null
}

// determine the released toolkit components
val releasedSourceSets = releasedModules.map { subproject ->
    // add all the intended library projects as sourceSets below
    File(rootDir, "toolkit/${subproject.name}/src/main/java").canonicalPath
}

tasks {
    //./gradlew :documentation:dokkaHtml
    // doc output will be under `documentation/build/dokka/html`.
    dokkaHtml {
        moduleName.set("arcgis-maps-kotlin-toolkit")
        dokkaSourceSets {
            configureEach {
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

    sourceSets {
        named("main") {
            java {
                releasedSourceSets.forEach {
                    srcDir(it)
                }
            }
        }
    }
}

dependencies {
    project.afterEvaluate {
       releasedModules.forEach { proj ->
           proj.configurations.forEach { config ->
               config.allDependencies.forEach {
                   //add all dependencies as implementation dependencies, no need for api.
                   project.dependencies.add("implementation", it)
               }
           }
       }
    }
}

