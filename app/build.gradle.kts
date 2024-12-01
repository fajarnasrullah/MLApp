plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.jer.mlapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jer.mlapp"
        minSdk = 24
        targetSdk = 34
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

    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    androidResources {
        noCompress ("tflite")
    }

    // import DownloadModels task
    val assetsDir = projectDir.resolve("/src/main/assets")
    val testAssetsDir = projectDir.resolve("/src/androidTest/assets")

// Download default models; if you wish to use your own models then
// place them in the "assets" directory and comment out this line.
//    apply (from = "download_models.gradle")



}

dependencies {

    //camera X
    implementation (libs.androidx.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.video)
    implementation (libs.androidx.camera.view)
    implementation (libs.androidx.camera.mlkit.vision)
    implementation (libs.androidx.camera.extensions)


    //TF Lite
    implementation ("org.tensorflow:tensorflow-lite-task-vision:0.4.0")
    // Import the GPU delegate plugin Library for GPU inference
    implementation ("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.9.0")

    //Google ML Kit Library

    //face detection
    implementation (libs.face.detection)

    //text recognition
    implementation ("com.google.mlkit:text-recognition:16.0.1")

    //camera source
    implementation (libs.play.services.mlkit.face.detection)

    // image labbeling
    implementation (libs.image.labeling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.vision.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}