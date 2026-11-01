import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

// Load local.properties for API keys (never committed)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.agrodoc.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agrodoc.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2-alpha"

        // BuildConfig fields for optional API keys
        buildConfigField("String", "OPENWEATHER_API_KEY",
            "\"${localProps.getProperty("openweather.api.key", "")}\"")
        buildConfigField("boolean", "FEATURE_ASSISTANT_LM", "false")

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Product flavors ────────────────────────────────────────────────────────
    flavorDimensions += "network"
    productFlavors {
        create("offline") {
            dimension = "network"
            applicationIdSuffix = ""
            versionNameSuffix = ""
            // Manifest overlay removes INTERNET if ever accidentally added
            // The offline merged manifest must NOT contain INTERNET (tested in unit test)
        }
        create("connected") {
            dimension = "network"
            applicationIdSuffix = ".connected"
            versionNameSuffix = "-connected"
            // AndroidManifest in connected source set declares INTERNET + ACCESS_NETWORK_STATE
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG_BUILD", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "DEBUG_BUILD", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("tflite", "bin")
    }

    // Room schema location for migrations
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    // ── Compose BOM ────────────────────────────────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Core / Activity ────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // ── Navigation ─────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ── Hilt ──────────────────────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.53.1")
    ksp("com.google.dagger:hilt-android-compiler:2.53.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Room ──────────────────────────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")

    // ── DataStore ─────────────────────────────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── CameraX ───────────────────────────────────────────────────────────────
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // ── Coil ──────────────────────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── LiteRT (TensorFlow Lite successor) ────────────────────────────────────
    implementation("com.google.ai.edge.litert:litert:1.4.0")

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ── Kotlin serialization (JSON parsing for knowledge base + weather) ───────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Networking classes and implementation exist only in the connected source set.
    add("connectedImplementation", "com.squareup.retrofit2:retrofit:2.11.0")
    add("connectedImplementation", "com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    add("connectedImplementation", "com.squareup.okhttp3:okhttp:4.12.0")
    add("connectedImplementation", "com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ── Permissions (Accompanist) ─────────────────────────────────────────────
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // ── Paging (for history list) ─────────────────────────────────────────────
    implementation("androidx.paging:paging-runtime-ktx:3.3.5")
    implementation("androidx.paging:paging-compose:3.3.5")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.12")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
