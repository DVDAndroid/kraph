pluginManagement {
  plugins {
    id("com.google.devtools.ksp") version "1.7.0-1.0.6"
    kotlin("jvm") version "1.7.0"
    kotlin("multiplatform") version "1.7.0"
  }
  repositories {
    gradlePluginPortal()
    google()
  }
}

include(":core")
include(":ksp-annotations")
include(":ksp-processor")
include(":ksp-example")