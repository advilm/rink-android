import org.asciidoctor.gradle.jvm.AsciidoctorTask
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    id("org.asciidoctor.jvm.convert.classic") version "5.0.3"
}

val rinkManualSourceDir = layout.buildDirectory.dir("generated/rink-manual/source")
val rinkManualAssetsDir = layout.buildDirectory.dir("generated/rink-manual/assets")
val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull

val downloadRinkManual = tasks.register("downloadRinkManual") {
    notCompatibleWithConfigurationCache("The manual download task uses script-defined task actions")
    outputs.dir(rinkManualSourceDir)
    outputs.upToDateWhen { false }

    doLast {
        val sourceDir = rinkManualSourceDir.get().asFile
        sourceDir.mkdirs()
        val manual = URI("https://codeberg.org/tiffany/rink/raw/branch/master/docs/rink.7.adoc").toURL()
            .openConnection()
            .apply {
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            .getInputStream()
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        sourceDir.resolve("rink.7.adoc").writeText(manual)
        sourceDir.resolve("manual.adoc").writeText(
            """= Rink Manual
:revnumber: 0.9.0
:website:
:toc:
:toclevels: 3

Documentation for Rink's query language.

include::rink.7.adoc[]
"""
        )
    }
}

tasks.named<AsciidoctorTask>("asciidoctor") {
    notCompatibleWithConfigurationCache("Asciidoctor Gradle plugin 5.0.3 is not configuration-cache compatible")
    dependsOn(downloadRinkManual)
    sourceDir(rinkManualSourceDir.get().asFile)
    baseDirFollowsSourceDir()
    setOutputDir(layout.buildDirectory.dir("generated/rink-manual/html").get().asFile)
    outputOptions {
        separateOutputDirs = false
    }
}

val bundleRinkManual = tasks.register<Copy>("bundleRinkManual") {
    dependsOn(tasks.named("asciidoctor"))
    from(layout.buildDirectory.dir("generated/rink-manual/html")) {
        include("manual.html")
    }
    into(rinkManualAssetsDir)
}

android {
    namespace = "net.adilm.rink"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "net.adilm.rink"
        minSdk = 28
        targetSdk = 36
        versionCode = providers.environmentVariable("ANDROID_VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("ANDROID_VERSION_NAME").orNull?.removePrefix("v") ?: "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("main").assets.directories.add(rinkManualAssetsDir.get().asFile.absolutePath)

    signingConfigs {
        if (!releaseKeystorePath.isNullOrBlank()) {
            create("ciRelease") {
                storeFile = file(releaseKeystorePath)
                storePassword = providers.environmentVariable("ANDROID_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            if (!releaseKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("ciRelease")
            }
            optimization {
                enable = true
                packageScope = setOf("net.adilm.rink.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.named("preBuild").configure {
    dependsOn(bundleRinkManual)
}

dependencies {
    implementation("net.java.dev.jna:jna:5.17.0@aar")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
