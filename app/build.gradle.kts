plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinKapt) // <-- Use KAPT instead of KSP
}

android {
    namespace = "com.insamt.nefroscan"
    compileSdk = 34

    @Suppress("DEPRECATION")
    aaptOptions {
        noCompress("tflite")
    }

    defaultConfig {
        applicationId = "com.insamt.nefroscan"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // Librerías Base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // TensorFlow Lite (IA On-Device) con exclusión segura integrada
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "com.google.flatbuffers", module = "flatbuffers-java")
    }
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0-rc2") {
        exclude(group = "com.google.flatbuffers", module = "flatbuffers-java")
    }

    // SceneView 3D (Gemelo Digital) con exclusión segura integrada
    implementation("io.github.sceneview:arsceneview:0.10.2") {
        exclude(group = "com.google.flatbuffers", module = "flatbuffers-java")
    }

    // Room Database (Configurado mediante KAPT)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion") // <-- Cambiado de ksp a kapt

    // Firebase (Plataforma Nube mediante BoM)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}