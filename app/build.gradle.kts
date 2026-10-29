import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    // kapt ships inside the kotlin-gradle-plugin jar; version comes from the
    // root project's org.jetbrains.kotlin.android declaration.
    id("org.jetbrains.kotlin.kapt")
}

// ---------------------------------------------------------------------------
// Backend API base URL, baked into BuildConfig.API_BASE_URL.
//
// Resolution order (first non-blank wins):
//   1. -PAPI_BASE_URL=https://...        (command line / CI)
//   2. API_BASE_URL=... in local.properties  (developer-local, gitignored)
//   3. API_BASE_URL=... in gradle.properties (committed default)
//   4. Fallback below
// A trailing slash is required by Retrofit and added automatically.
// ---------------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val resolvedApiBaseUrl = listOf(
    gradle.startParameter.projectProperties["API_BASE_URL"],
    localProperties.getProperty("API_BASE_URL"),
    providers.gradleProperty("API_BASE_URL").orNull,
)
    .firstOrNull { !it.isNullOrBlank() }
    ?.trim()
    ?.replace("\"", "")
    ?: "https://second-hand-market-api.onrender.com/"
val apiBaseUrl = if (resolvedApiBaseUrl.endsWith("/")) {
    resolvedApiBaseUrl
} else {
    "$resolvedApiBaseUrl/"
}

android {
    namespace = "com.secondhandmarket"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.secondhandmarket"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Injected from the resolution logic above (see DEPLOYMENT.md).
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // For Firebase Phone Auth
        manifestPlaceholders["siteKey"] = ""
        manifestPlaceholders["recaptchaSecret"] = ""
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        // Compose compiler 1.5.5 matches Kotlin 1.9.20 (1.5.6 requires 1.9.21)
        kotlinCompilerExtensionVersion = "1.5.5"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Networking (Retrofit + OkHttp)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Image Loading (Coil for Compose)
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Firebase (Auth, Firestore, Messaging)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    
    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}

detekt {
    toolVersion = "1.23.4"
    config = files("../config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    allRules = true
    baseline = file("../config/detekt/baseline.xml")
}