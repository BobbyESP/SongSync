plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.serialization)
    alias(libs.plugins.parcelize)
}

val releaseStoreFile = project.properties["RELEASE_STORE_FILE"] as String?
val releaseStorePassword = project.properties["RELEASE_STORE_PASSWORD"] as String?
val releaseKeyAlias = project.properties["RELEASE_KEY_ALIAS"] as String?
val releaseKeyPassword = project.properties["RELEASE_KEY_PASSWORD"] as String?

android {
    namespace = "pl.lambada.songsync"
    compileSdk = 34

    defaultConfig {
        applicationId = "pl.lambada.songsync"
        minSdk = 21
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 303
        versionName = "3.0.3"

        resourceConfigurations += arrayOf(
            "ar", "de", "en", "es", "in", "ja", "pt", "pt-rBR", "ro", "vi", "zh-rCN", "tr"
        )

        vectorDrawables {
            useSupportLibrary = true
        }
    }
    signingConfigs {
        create("release") {
            if (project.hasProperty("RELEASE_KEY_ALIAS")) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (project.hasProperty("RELEASE_KEY_ALIAS")) {
                signingConfig = signingConfigs["release"]
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.tooling.preview)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.preference)
    implementation(libs.ktor.core)
    implementation(libs.ktor.cio)
}