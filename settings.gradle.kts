pluginManagement {
  plugins {
    id("com.google.devtools.ksp") version "1.6.20-1.0.5"
    kotlin("jvm") version "1.6.20"
    kotlin("multiplatform") version "1.6.20"
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