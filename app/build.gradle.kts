import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * The Android debug keystore normally lives in `~/.android`, which is not writable in every
 * environment (sandboxes, locked-down CI images). When `local.properties` points at a
 * keystore, that one is used instead; otherwise AGP generates the usual default.
 *
 * `local.properties` is a Java properties file, so `\` and `:` arrive escaped and have to be
 * unescaped before the value is a usable path.
 */
val localDebugKeystore: File? = rootProject.file("local.properties")
    .takeIf { it.isFile }
    ?.readLines()
    ?.asSequence()
    ?.map(String::trim)
    ?.firstOrNull { it.startsWith("debug.keystore=") }
    ?.substringAfter('=')
    ?.trim()
    ?.replace("\\\\", "\\")
    ?.replace("\\:", ":")
    ?.takeIf { it.isNotEmpty() }
    ?.let(::File)
    ?.takeIf { it.isFile }

if (localDebugKeystore != null) {
    // `android.debug.keystore` is only settable from the command line / gradle.properties,
    // so an unset one is filled in here to keep the build self-contained.
    if (System.getProperty("android.debug.keystore") == null) {
        System.setProperty("android.debug.keystore", localDebugKeystore.absolutePath)
    }
    logger.lifecycle("TimeBlock: signing debug builds with ${localDebugKeystore.absolutePath}")
}

android {
    namespace = "com.wavachao.timeblock"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wavachao.timeblock"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    if (localDebugKeystore != null && localDebugKeystore.exists()) {
        signingConfigs {
            getByName("debug") {
                storeFile = localDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Signed with the debug keystore so the CI artifact installs straight
            // from the browser. Replace with a real upload key before publishing.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
