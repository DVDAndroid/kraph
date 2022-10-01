plugins {
  kotlin("jvm") version "1.7.0" apply false
  kotlin("multiplatform") version "1.7.0" apply false
  `maven-publish`
}

allprojects {
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

buildscript {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
  }
}

subprojects {
  group = "com.dvdandroid.kraph"
  version = "0.7.6-kmp-ksp-SNAPSHOT"

  if ("example" !in name) {
    apply(plugin = "maven-publish")

    publishing {
      repositories {
        if (System.getenv("CI")?.toBoolean() == true) {
          maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/dvdandroid/kraph")
            credentials {
              username = System.getenv("GITHUB_ACTOR")
              password = System.getenv("GITHUB_TOKEN")
            }
          }
        }
      }
    }
  }
}