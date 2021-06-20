plugins {
    kotlin("multiplatform") version "1.5.10" apply false
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