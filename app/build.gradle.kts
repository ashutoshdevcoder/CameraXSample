plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ashdev.cameraxsample"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ashdev.cameraxsample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    dataBinding {
        enable = true
    }

    buildFeatures {
        viewBinding  = true
        dataBinding =  true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // CameraX core library
    implementation (libs.androidx.camera.core)

    // CameraX Camera2 implementation
    implementation (libs.camera.camera2)

    // CameraX Lifecycle library
    implementation (libs.camera.lifecycle)

    // CameraX View class
    implementation (libs.androidx.camera.view)

    // For permission handling
    implementation (libs.androidx.activity.ktx)
    implementation (libs.androidx.fragment.ktx)

    // For image processing (optional)
    implementation (libs.androidx.concurrent.futures.ktx)
    implementation (libs.glide)

}