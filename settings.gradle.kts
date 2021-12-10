pluginManagement {
  plugins {
    id("com.google.devtools.ksp") version "1.6.0-1.0.1"
    kotlin("jvm") version "1.6.0"
    kotlin("multiplatform") version "1.6.0"
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