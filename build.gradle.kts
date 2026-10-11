import com.android.build.api.dsl.LibraryExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        classpath("com.lagradost.cloudstream3:gradle:local")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "com.android.library")

    apply(plugin = "com.lagradost.cloudstream3.gradle")

    extensions.getByName<CloudstreamExtension>("cloudstream").apply {
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "Frenk0371/frenky-extra")
    }

    extensions.getByType<LibraryExtension>().apply {
        namespace = "com.frenky.extra.${project.name.lowercase()}"
        compileSdk = 35
        defaultConfig {
            minSdk = 21
            
            
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    tasks.withType<KotlinJvmCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.addAll("-Xno-call-assertions", "-Xno-param-assertions")
        }
    }

    dependencies {
        val cloudstream by configurations
        val implementation by configurations
        cloudstream("com.lagradost:cloudstream3:pre-release")
        implementation(kotlin("stdlib"))
        implementation("org.jsoup:jsoup:1.18.3")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
