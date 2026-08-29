plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.basemax.smsforwarder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.basemax.smsforwarder"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // The UI is English-only. Without this every AndroidX library drags in
        // its full set of translations, none of which this app can show.
        resourceConfigurations += "en"
    }

    buildTypes {
        // R8 runs on debug as well. The debug APK is what CI hands people, so
        // it has no business being several times the size of a release build.
        // isDebuggable stays on, so it is still debuggable and debug-signed.
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            // Live literals wrap every constant in a lookup class so the IDE can
            // swap them without recompiling. Nothing here needs that, and it is
            // a per-file class plus a field per literal in the APK.
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:liveLiteralsEnabled=false",
            // Source markers exist for the Compose Layout Inspector, which needs
            // the ui-tooling runtime we no longer ship.
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=false",
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Compatible with Kotlin 1.9.24.
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/*.version",
            "/META-INF/*.kotlin_module",
            "/META-INF/com/android/build/gradle/*",
            "/DebugProbesKt.bin",
            // Kotlin builtin metadata, only read by kotlin-reflect.
            "/kotlin/**",
            "**/*.proto",
        )
    }
    // The dependency blob signed into the APK is only for Play Console.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    // No material-icons-extended: it ships every Material icon there is. The
    // twelve this app draws live in ui/icons/AppIcons.kt.
    // No ui-tooling / ui-tooling-preview either - there are no @Preview
    // functions. Add both back (debugImplementation) if you want previews.

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // HTTP is HttpURLConnection + org.json, both part of Android. See ApiClient.

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
}
