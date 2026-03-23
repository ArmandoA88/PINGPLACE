plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.pingplace"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pingplace"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "PLACES_API_KEY",
            "\"${project.findProperty("PLACES_API_KEY") as? String ?: ""}\""
        )
        buildConfigField(
            "String",
            "OFFLINE_PACK_MANIFEST_URL",
            "\"${project.findProperty("OFFLINE_PACK_MANIFEST_URL") as? String ?: ""}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
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

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.compose.material:material"))
            .using(module("androidx.compose.material:material-android:1.7.6"))
        substitute(module("androidx.compose.material:material-ripple"))
            .using(module("androidx.compose.material:material-ripple-android:1.7.6"))
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx-android:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose-android:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.runtime:runtime-android:1.7.6")
    implementation("androidx.compose.foundation:foundation-android:1.7.6")
    implementation("androidx.compose.foundation:foundation-layout-android:1.7.6")
    implementation("androidx.compose.ui:ui-android:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.7.6")
    implementation("androidx.compose.material3:material3-android:1.3.1")
    implementation("androidx.compose.material:material-icons-extended-android:1.7.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")

    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
}
