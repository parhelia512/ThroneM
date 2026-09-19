plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.aboutlibraries)
}

val packageNameProvider = requireMetadata("PACKAGE_NAME")
val versionNameProvider = requireMetadata("VERSION_NAME")
val versionCodeProvider = requireMetadata("VERSION_CODE")

val libboxAarFile = layout.projectDirectory.file("libs/libbox.aar").asFile
val checkLibboxAar = tasks.register("checkLibboxAar") {
    description = "Fails with an explanation when the libbox AAR is missing."
    val aarPath = libboxAarFile.path
    doFirst {
        if (!File(aarPath).isFile) {
            error(
                "Missing libbox AAR '$aarPath'. Download libbox.aar from the " +
                    "sing-box-lx release page and place it in composeApp/libs/.",
            )
        }
    }
}


val bundledAssetFiles = listOf("geoip.db.gz", "geosite.db.gz")
val bundledAssetsDir = layout.projectDirectory.dir("src/commonMain/composeResources/files/sing-box").asFile
val warnMissingAssets = tasks.register("warnMissingBundledAssets") {
    description = "Warns when the geoip/geosite assets have not been downloaded yet."
    val assetsDirPath = bundledAssetsDir.path
    val assetFileNames = bundledAssetFiles.toList()
    doFirst {
        val missing = assetFileNames.filterNot { File(assetsDirPath, it).isFile }
        if (missing.isNotEmpty()) {
            logger.warn(
                "Missing bundled route assets in '$assetsDirPath': ${missing.joinToString()}. " +
                    "The app will ship without geoip/geosite rule sets; run 'make assets' to download them.",
            )
        }
    }
}

tasks.matching { it.name.startsWith("compile") }.configureEach {
    if (name.contains("Android", ignoreCase = true)) {
        dependsOn(checkLibboxAar)
    }
    dependsOn(warnMissingAssets)
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    description = "Generates the shared BuildConfig Kotlin source."
    val outputDir = layout.buildDirectory.dir("generated/buildConfig/kotlin")
    val versionName = versionNameProvider.get()
    val versionCode = versionCodeProvider.get()
    inputs.property("versionName", versionNameProvider)
    inputs.property("versionCode", versionCodeProvider)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("io/throneproj/thronem")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            |package io.throneproj.thronem
            |
            |object BuildConfig {
            |    const val VERSION_NAME = "$versionName"
            |    const val VERSION_CODE = $versionCode
            |    const val FLAVOR = ""
            |}
            """.trimMargin(),
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }

    android {
        namespace = "io.throneproj.thronem.lib"
        buildToolsVersion = "37.0.0"
        compileSdk = 37
        minSdk = 24
        androidResources {
            enable = true
        }
    }

    sourceSets {
        getByName("commonMain") {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.animation.graphics)
                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.ui.tooling.preview)
                implementation(libs.jetbrains.lifecycle.viewmodel.compose)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.jetbrains.lifecycle.runtime.compose)
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                implementation(libs.ini4j)
                implementation(libs.okio)
                implementation(libs.compose.preference)
                implementation(libs.fastscroller.core)
                implementation(libs.fastscroller.material3)
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs.compose)
                implementation(libs.aboutlibraries.compose.m3)
                implementation(libs.zxing.core)
                implementation(project(":library:DragDropSwipeLazyColumn"))

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.core.viewmodel)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose.navigation3)
            }
        }
        getByName("androidMain") {
            languageSettings.optIn("androidx.tv.material3.ExperimentalTvMaterial3Api")
            dependencies {
                implementation(
                    fileTree("libs") {
                        include("*.aar")
                    },
                )

                implementation(libs.kotlinx.coroutines.android)

                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.work.multiprocess)
                implementation(libs.androidx.datastore)
                implementation(libs.androidx.compose.ui.tooling)
                implementation(libs.androidx.activity.compose)

                implementation(libs.koin.android)

                implementation(libs.accompanist.drawablepainter)

                implementation(libs.androidx.camera.core)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.compose)

                implementation(libs.smali.dexlib2.get().toString()) {
                    exclude(group = "com.google.guava", module = "guava")
                }

                implementation(libs.process.phoenix)

                implementation(libs.androidx.tv.material)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "io.throneproj.thronem.resources"
}

val commonAboutLibrariesDir = layout.projectDirectory.dir("src/commonMain/aboutlibraries")

aboutLibraries {
    offlineMode = true
    collect {
        configPath = commonAboutLibrariesDir.asFile
    }
    export {
        variant = "android"
        outputFile = file("src/androidMain/composeResources/files/aboutlibraries.json")
    }
}

ksp {
    arg("room.incremental", "true")
    arg("room.schemaLocation", "${projectDir}/schemas")
}

dependencies {
    kspAndroid(libs.androidx.room.compiler)
}
