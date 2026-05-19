plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.monitorplugin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.monitorplugin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        aidl = true
    }
}

// Standalone-mode guard: release builds with R8, obfuscation, and APK signing are
// handled exclusively by the superproject. Disable the release variant when building
// standalone so the task graph stays clean and consistent.
if (findProject(":mondonode-sdk") == null) {
    androidComponents {
        beforeVariants { variantBuilder ->
            if (variantBuilder.buildType == "release") variantBuilder.enable = false
        }
    }
}

dependencies {
    implementation(
        if (findProject(":mondonode-sdk") != null) project(":mondonode-sdk")
        else "com.systemhalted.mondonode:mondonode-sdk:0.1.0"
    )
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines.android)
}

if (findProject(":mondonode-billing") == null) {
    android {
        sourceSets {
            named("main") {
                kotlin.srcDir("src/billing-disabled/kotlin")
            }
        }
    }
}
