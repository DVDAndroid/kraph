import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("com.google.devtools.ksp") version "1.6.0-1.0.1"
  `maven-publish`
}

buildscript {
  dependencies {
    classpath(kotlin("gradle-plugin", version = "1.6.0"))
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
}

dependencies {
  implementation(project(":core"))
  implementation(project(":ksp-annotations"))
  implementation("com.squareup:kotlinpoet:1.10.2")
  implementation("com.squareup:kotlinpoet-ksp:1.10.2")
  implementation("com.google.auto.service:auto-service-annotations:1.0.1")
  implementation("com.google.devtools.ksp:symbol-processing-api:1.6.0-1.0.1")
  ksp("dev.zacsweers.autoservice:auto-service-ksp:1.0.0")

  testImplementation(kotlin("test"))
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.6")
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.6")
}


tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.freeCompilerArgs += listOf(
    "-Xopt-in=com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview",
    "-Xopt-in=com.google.devtools.ksp.KspExperimental",
  )
}