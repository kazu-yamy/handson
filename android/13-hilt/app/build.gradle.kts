plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.handson.android01"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.handson.android01"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.handson.android01.HiltTestRunner"

        // AGP 8.0 以降は BuildConfig の生成が既定で無効（AGP 9 も同じ）。カタログの値を埋め込む
        buildConfigField("String", "KOTLIN_VERSION", "\"${libs.versions.kotlin.get()}\"")
        buildConfigField("String", "COMPOSE_BOM_VERSION", "\"${libs.versions.composeBom.get()}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
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
}

// Room のスキーマ（JSON）の出力先。Room 3 の Gradle プラグインでは指定が必須
room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    ksp(libs.androidx.room3.compiler)
    ksp(libs.hilt.android.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.ktor.client.mock)
    kspAndroidTest(libs.hilt.android.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}