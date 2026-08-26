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

import java.time.Year

plugins {
    id("com.android.library")
    alias(libs.plugins.dokka)
}

val versionNumber: String =
    providers.gradleProperty("versionNumber").orNull
        ?: rootProject.findProperty("versionNumber")?.toString()
        ?: error("Missing required property 'versionNumber'")

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
    // Runs after Dokka generation. Changes string "arcgis-maps-kotlin-toolkit" to "ArcGIS Maps SDK for Kotlin Toolkit" in
    // index.html only. Will display product name at the top of the landing page,
    // Using moduleName.set("ArcGIS Maps SDK for Kotlin Toolkit"), results in URLs too long. So we use moduleName.set("arcgis-maps-kotlin-toolkit"),
    // which results in a URL like this:
    // https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.geoviewcompose/-map-view.html.
    // Then this task changes "arcgis-maps-kotlin-toolkit" to "ArcGIS Maps SDK for Kotlin Toolkit" in the index.html only,
    // for display purposes.
    register("replaceToolkitApiRefName") {
        doLast {
            val indexFile = file("build/dokka/html/index.html")
            val newName = indexFile.readText()
                .replace(Regex(">arcgis-maps-kotlin-toolkit</"), ">ArcGIS Maps SDK for Kotlin Toolkit</")
            indexFile.writeText(newName)
        }
    }
}

// To generate the KDoc for Toolkit project, run: `./gradlew :kdoc:dokkaGenerate`
dokka {
    dokkaPublications.html {
        pluginsConfiguration.versioning {
            version = versionNumber
        }
        pluginsConfiguration.html {
            separateInheritedMembers = true
            failOnWarning = false
        }
        moduleName.set("arcgis-maps-kotlin-toolkit")
        moduleVersion.set(versionNumber)

        dokkaSourceSets.configureEach {
            sourceRoots.from(releasedSourceSetPaths)

            perPackageOption {
                matchingRegex.set(".*internal.*")
                suppress.set(true)
                reportUndocumented = true
            }
        }
        pluginsConfiguration.html {
            footerMessage.set("Copyright © ${Year.now().value} Esri. All Rights Reserved.")
            homepageLink = "https://developers.arcgis.com/kotlin/"
            customAssets.from(
                project.layout.projectDirectory.file("dokka-assets/logo-icon.svg")
            )
        }
    }
}

val dokkaGenerationTasks = setOf(
    tasks.dokkaGenerate.get(),
    tasks.dokkaGenerateHtml.get(),
    tasks.dokkaGeneratePublicationHtml.get()
)
tasks.matching { it in dokkaGenerationTasks }.configureEach {
    finalizedBy(tasks.named("replaceToolkitApiRefName"))
}

android {
    namespace = "com.arcgismaps.toolkit.doc"
    compileSdk = libs.versions.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

dependencies {
    // Puts the version in the KDoc
    dokkaPlugin(libs.dokka.versioning)
    // put exposed dependencies in dokka's classpath
    implementation(arcgis.mapsSdk)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.composeCore)
}
