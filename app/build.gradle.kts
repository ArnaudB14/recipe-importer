plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.isariand.recettes"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.isariand.recettes"
        minSdk = 26
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

    val geminiApiKey = providers.gradleProperty("GEMINI_API_KEY")
    if (geminiApiKey.isPresent) {
        buildTypes.all {
            resValue("string", "gemini_api_key", "\"${geminiApiKey.get()}\"")
        }
    }

    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
    // MVVM & Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // RETROFIT (Client HTTP)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // MOSHI (Convertisseur JSON vers Kotlin Data Classes)
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1") // Pour la génération automatique

    // OKHTTP (Pour voir les logs des requêtes, utile pour le débogage)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    val room_version = "2.6.1" // Définition de la version Room

    // Room - Runtime et Implémentation de la base de données
    implementation("androidx.room:room-runtime:$room_version")

    // Room - KTX (pour le support des Coroutines et Flow)
    implementation("androidx.room:room-ktx:$room_version")

    // Room - Annotation Processor (nécessaire pour générer la base de données)
    kapt("androidx.room:room-compiler:$room_version")

    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.google.ai.client.generativeai:generativeai:latest.release")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}