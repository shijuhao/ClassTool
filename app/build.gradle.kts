import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    compileSdk = 36

    namespace = "com.juhao.classtool"

    defaultConfig {
        applicationId = "com.juhao.classtool"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.0.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        localeFilters.clear()
        localeFilters.addAll(listOf("zh", "en"))
    }
    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/*.kotlin_module",
                    "META-INF/kotlin-tooling-metadata.json",
                    "META-INF/*.version",
                    "META-INF/versions/**",
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                    "META-INF/AL2.0",
                    "META-INF/LGPL2.1"
            ))
        }
        jniLibs {
            useLegacyPackaging = true
        }
        dex {
            useLegacyPackaging = true
        }
    }
}

dependencies {

    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)
    implementation(libs.androidx.activity.compose)

    implementation(libs.wear.compose.material)

    implementation(libs.wear.compose.foundation)
    implementation(libs.androidx.material.icons.core)

    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.tooling)

    implementation(libs.wear.compose.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.datastore.preferences)
    implementation(libs.datastore)

    implementation(libs.androidx.ui.test.manifest)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.rule)

    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(composeBom)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(composeBom)
}
