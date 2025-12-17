import java.util.Properties

fun loadLocalProperty(key: String): String {
    val props = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { props.load(it) }
    }
    return props.getProperty(key) ?: ""
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val openaiApiKey: String = localProperties.getProperty("OPENAI_API_KEY") ?: ""

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
        val geminiApiKey = loadLocalProperty("GEMINI_API_KEY")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"$openaiApiKey\"")
        val groqKey = loadLocalProperty("GROQ_API_KEY")
        buildConfigField("String", "GROQ_API_KEY", "\"$groqKey\"")
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
        buildConfig = true
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
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.google.ai.client.generativeai:generativeai:latest.release")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

}