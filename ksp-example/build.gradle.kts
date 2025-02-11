plugins {
  kotlin("multiplatform")
  id("com.google.devtools.ksp")
}

kotlin {
  jvm()
  js(IR) {
    browser()
    nodejs()
    binaries.executable()
  }

  @Suppress("UNUSED_VARIABLE")
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project(":ksp-annotations"))
        implementation(project(":core"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}

// Generate common code with ksp instead of per-platform, hopefully this won't be needed in the future.
// https://github.com/google/ksp/issues/567
kotlin.sourceSets.commonMain {
  kotlin.srcDir("build/generated/ksp/common/main/kotlin")
  kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin") // for ide
}

dependencies {
  add("kspCommonMainMetadata", project(":ksp-processor"))
}

ksp {
  arg("kraph.packageName", "com.test")
}