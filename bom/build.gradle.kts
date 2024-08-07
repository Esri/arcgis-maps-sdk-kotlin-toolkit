/*
 *
 *  Copyright 2023 Esri
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
    id("maven-publish")
    id("java-platform")
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
val finalBuild: String by project
val artifactVersion: String = if (finalBuild == "true") {
    versionNumber
} else {
    "$versionNumber-$buildNumber"
}

// ensure that the evaluation of the bom project happens after all other projects
// so that plugins are applied to all projects, and can be used to identify
// which projects should get written into the BOM's pom file.
rootProject.subprojects.filter {
    it.name != project.name
}.forEach {
    evaluationDependsOn(":${it.name}")
}

// now find projects which are publishable based on their inclusion
// of the publishing plugin, and add them as api dependencies.
dependencies {
    constraints {
        project.rootProject.subprojects.filter {
            it.plugins.findPlugin("artifact-deploy") != null
        }.forEach { subproject ->
            // add all the intended library projects as api dependencies.
            api(subproject)
        }
    }
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
            create<MavenPublication>("bom") {
                groupId = artifactoryGroupId
                artifactId = artifactoryArtifactId
                version = artifactVersion
                
                from(components["javaPlatform"])
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


