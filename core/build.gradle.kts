plugins {
    kotlin("multiplatform")
    `maven-publish`
}
group = "com.dvdandroid.kraph"
version = "0.6.1-kmp"

kotlin {
    jvm()
    js(BOTH) {
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