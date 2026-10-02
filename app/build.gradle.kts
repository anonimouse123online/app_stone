plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")

    id("com.google.devtools.ksp") version "2.3.10"
}


android {

    namespace =
        "com.example.capstonesample"


    compileSdk {

        version = release(36) {

            minorApiLevel = 1
        }
    }


    defaultConfig {

        applicationId =
            "com.example.capstonesample"

        minSdk =
            24

        targetSdk =
            36

        versionCode =
            1

        versionName =
            "1.0"


        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {

        release {

            optimization {

                enable =
                    false
            }
        }
    }


    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }


    buildFeatures {

        compose =
            true
    }


    // ============================================================
    // DO NOT COMPRESS AI MODELS
    // ============================================================

    androidResources {

        noCompress +=
            "tflite"


        noCompress +=
            "litertlm"
    }
}



dependencies {

    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-messaging")


    // ============================================================
    // JETPACK COMPOSE
    // ============================================================

    implementation(

        platform(
            libs.androidx.compose.bom
        )
    )


    implementation(
        libs.androidx.activity.compose
    )


    implementation(
        libs.androidx.compose.material3
    )


    implementation(
        libs.androidx.compose.ui
    )


    implementation(
        libs.androidx.compose.ui.graphics
    )


    implementation(
        libs.androidx.compose.ui.tooling.preview
    )


    implementation(
        libs.androidx.core.ktx
    )


    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )


    implementation(

        "androidx.compose.material:material-icons-extended:1.7.8"
    )

    implementation("io.coil-kt:coil-compose:2.7.0")



    // ============================================================
    // RETROFIT
    // ============================================================

    implementation(

        "com.squareup.retrofit2:retrofit:2.11.0"
    )


    implementation(

        "com.squareup.retrofit2:converter-gson:2.11.0"
    )



    // ============================================================
    // OKHTTP
    // ============================================================

    implementation(

        "com.squareup.okhttp3:okhttp:4.12.0"
    )


    implementation(

        "com.squareup.okhttp3:logging-interceptor:4.12.0"
    )



    // ============================================================
    // WORKMANAGER
    // ============================================================

    implementation(

        "androidx.work:work-runtime-ktx:2.11.2"
    )



    // ============================================================
    // KOTLIN COROUTINES
    //
    // REQUIRED BY LITERT-LM 0.14.0
    //
    // Fixes:
    //
    // NoSuchMethodError
    // SendChannel.close$default(...)
    //
    // ============================================================

    implementation(

        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0"
    )


    implementation(

        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0"
    )



    // ============================================================
    // ROOM DATABASE
    // ============================================================

    implementation(

        "androidx.room:room-runtime:2.7.2"
    )


    implementation(

        "androidx.room:room-ktx:2.7.2"
    )


    ksp(

        "androidx.room:room-compiler:2.7.2"
    )



    // ============================================================
    // YOLO / LITERT
    //
    // LiteRT 2.1 Kotlin package already provides
    // accelerator support.
    //
    // Do NOT add litert-gpu-api:2.1.0
    // ============================================================

    implementation(

        "com.google.ai.edge.litert:litert:2.1.0"
    )



    // ============================================================
    // GEMMA / LITERT-LM
    // ============================================================

    implementation(

        "com.google.ai.edge.litertlm:litertlm-android:0.14.0"
    )



    // ============================================================
    // EXIF
    // ============================================================

    implementation(

        "androidx.exifinterface:exifinterface:1.3.7"
    )



    // ============================================================
    // TESTING
    // ============================================================

    testImplementation(

        libs.junit
    )


    androidTestImplementation(

        platform(
            libs.androidx.compose.bom
        )
    )


    androidTestImplementation(

        libs.androidx.compose.ui.test.junit4
    )


    androidTestImplementation(

        libs.androidx.espresso.core
    )


    androidTestImplementation(

        libs.androidx.junit
    )


    debugImplementation(

        libs.androidx.compose.ui.test.manifest
    )


    debugImplementation(

        libs.androidx.compose.ui.tooling
    )
}



// ============================================================
// FORCE COROUTINES VERSION
//
// Prevents another library from resolving an older
// kotlinx-coroutines runtime.
// ============================================================

configurations.all {

    resolutionStrategy {


        force(

            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0"
        )


        force(

            "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0"
        )


        force(

            "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0"
        )
    }
}