# MusicMinds Android Project Setup Guide

This guide will help you set up the MusicMinds Android project correctly so that development agents can implement the user stories starting with US01 (Spotify Authentication).

## Android Studio Project Creation

### Step 1: Create New Project
1. **Open Android Studio**
2. **Select "New Project"**
3. **Choose template**: "Empty Views Activity"
4. **Project Configuration**:
   - **Name**: `MusicMinds`
   - **Package name**: `com.kevindagame.musicminds`
   - **Save location**: `E:\Programming\MusicMind\app` (or your preferred path)
   - **Language**: `Kotlin`
   - **Minimum SDK**: `API 34 (Android 14)`
   - **Build configuration language**: `Kotlin DSL (build.gradle.kts)`

### Benefits of SDK 34 (Android 14)
- **Enhanced Privacy**: More granular permissions and privacy controls
- **Improved Performance**: Better runtime optimizations and memory management
- **Modern Security**: Latest security features and cryptographic improvements
- **Better Battery Management**: Optimized background processing and power efficiency
- **Notification Management**: Advanced notification controls and user experience
- **Covers ~40-50% of active Android devices** (as of late 2024)
- **Future-ready**: Aligns with modern Android development practices

> **Note**: While this reduces device compatibility compared to lower SDK versions, it provides access to modern Android features and ensures the app is built with current security and performance standards.

### Step 2: Project Structure
Your project should have this structure:
```
MusicMind/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kevindagame/musicminds/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── Documentation/ (existing)
├── UserStories/ (existing)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties
└── .gitignore (existing)
```

## Required Dependencies

### Step 3: Root build.gradle.kts
Create `build.gradle.kts` in the root directory:

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.gradle) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}
```

### Step 4: App-level build.gradle.kts
Your `app/build.gradle.kts` should include:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.kevindagame.musicminds"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kevindagame.musicminds"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Retrofit for additional API calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Spotify Android SDK (add manually)
    implementation(files("libs/spotify-android-auth-1.2.6.aar"))
    implementation(files("libs/spotify-android-appremote-1.2.6.aar"))
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### Step 5: Version Catalog (libs.versions.toml)
Create `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.7.0"
kotlin = "2.0.20"
coreKtx = "1.13.1"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
lifecycleRuntimeKtx = "2.8.6"
activityCompose = "1.9.2"
composeBom = "2024.10.00"
navigationCompose = "2.8.1"
lifecycleViewmodelCompose = "2.8.6"
hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
roomVersion = "2.6.1"
retrofit = "2.11.0"
okhttpLoggingInterceptor = "4.12.0"
coroutines = "1.8.1"
datastorePreferences = "1.1.1"
coilCompose = "2.7.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "roomVersion" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "roomVersion" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "roomVersion" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttpLoggingInterceptor" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coilCompose" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-gradle = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
```

## Spotify SDK Setup

### Step 6: Download Spotify SDK
1. **Go to**: https://github.com/spotify/android-sdk/releases
2. **Download** the latest release (currently 1.2.6)
3. **Extract** the AAR files to `app/libs/` directory:
   - `spotify-android-auth-1.2.6.aar`
   - `spotify-android-appremote-1.2.6.aar`

### Step 7: Spotify Developer Account
1. **Visit**: https://developer.spotify.com/dashboard
2. **Create an app** with these settings:
   - **App name**: MusicMinds
   - **App description**: Song learning flashcard app
   - **Website**: (optional)
   - **Redirect URI**: `com.kevindagame.musicminds://callback`
3. **Note your Client ID** - you'll need this for the app

## Android Manifest Configuration

### Step 8: AndroidManifest.xml
Configure `app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        android:name=".MusicMindsApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.MusicMinds">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.MusicMinds">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- Spotify callback activity -->
        <activity
            android:name="com.spotify.sdk.android.auth.LoginActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="com.kevindagame.musicminds" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
```

## Initial Project Files

This setup guide ensures your project is ready for US01 (Spotify Authentication) implementation. The agent will have all necessary dependencies, proper architecture foundation, and Spotify SDK integration ready to go.

## Next Steps for Agent

Once this setup is complete, an agent can start implementing US01 by:
1. Creating the authentication flow
2. Implementing secure token storage
3. Building the authentication UI
4. Testing the Spotify connection

The project structure follows modern Android development best practices with:
- ✅ Clean Architecture (MVVM)
- ✅ Dependency Injection (Hilt)
- ✅ Jetpack Compose for UI
- ✅ Room for local database
- ✅ Coroutines for async operations
- ✅ Spotify SDK integration ready