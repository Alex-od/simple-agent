import java.util.Properties

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.danichapps.simpleagent"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.danichapps.simpleagent"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENAI_API_KEY", "\"${localProps["OPENAI_API_KEY"]}\"")
        buildConfigField("String", "ON_DEVICE_LLM_MODEL_FILENAME", "\"${localProps["ON_DEVICE_LLM_MODEL_FILENAME"] ?: "Qwen2.5-3B-Instruct-Q4_K_M.gguf"}\"")
        buildConfigField("String", "ON_DEVICE_LLM_MODEL_PATH", "\"${localProps["ON_DEVICE_LLM_MODEL_PATH"] ?: ""}\"")
        buildConfigField("String", "LOCAL_EMBEDDING_MODEL_FILENAME", "\"${localProps["LOCAL_EMBEDDING_MODEL_FILENAME"] ?: ""}\"")
        buildConfigField("String", "LOCAL_EMBEDDING_MODEL_PATH", "\"${localProps["LOCAL_EMBEDDING_MODEL_PATH"] ?: ""}\"")
        buildConfigField("String", "LOCAL_SERVER_BASE_URL", "\"${localProps["LOCAL_SERVER_BASE_URL"] ?: "http://10.0.2.2:8080/v1"}\"")
        buildConfigField("String", "LOCAL_SERVER_MODEL", "\"${localProps["LOCAL_SERVER_MODEL"] ?: "Qwen2.5-3B-Instruct-Q4_K_M"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.documentfile)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)
    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
