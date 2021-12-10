plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()
  js(IR) {
    browser()
  }
  tasks.withType<Test> {
    useJUnitPlatform {
      include("spek")
      includeEngines("spek")
    }
  }

  @Suppress("UNUSED_VARIABLE")
  sourceSets {
    val commonMain by getting
    val jvmTest by getting {
      dependencies {
        implementation("org.jetbrains.spek:spek-api:1.1.5")
        runtimeOnly("org.jetbrains.spek:spek-junit-platform-engine:1.1.5")
        implementation("com.natpryce:hamkrest:1.8.0.1")
        runtimeOnly(kotlin("reflect"))
      }
    }
  }
}