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
// TODO: figure out versioning
val artifactVersion: String = "xx.xx"

dependencies {
     constraints {
          project.rootProject.subprojects.forEach {subproject ->
               if (!subproject.name.endsWith("-app") && subproject.name != "composable-map" && subproject.name != "bom") {
                    // add all the library projects as api dependencies.
                    api(subproject)
               }
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
