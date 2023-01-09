buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    }
    dependencies {
        classpath("org.daiv.dependency:DependencyHandling:0.1.43")
    }
}
val versions = org.daiv.dependency.DefaultDependencyBuilder()
plugins {
    kotlin("multiplatform") version "1.6.10"
//    kotlin("kapt") version "1.6.10"
//    id("com.jfrog.artifactory") version "4.17.2"
    id("org.daiv.dependency.VersionsPlugin") version "0.1.4"
    kotlin("plugin.serialization") version "1.4.0"
    `maven-publish`
}



group = "org.daiv.persistence"
version = "0.0.1"
//versionPlugin {
//    versionPluginBuilder = org.daiv.dependency.Versions.versionPluginBuilder {
//        versionMember = { jpersistence }
//        resetVersion = { copy(jpersistence = it) }
//    }
//    setDepending(tasks)
//}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(LEGACY) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }

    
    sourceSets {
        val commonMain by getting{
            dependencies {
                implementation(versions.serialization())
                implementation(versions.coroutines())
                implementation(versions.coroutines_lib())
                implementation(versions.kutil())
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting{
            dependencies {
                implementation(kotlin("reflect"))
                implementation(versions.sqlite_jdbc())
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
