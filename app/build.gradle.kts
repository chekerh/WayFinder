plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21" // Add serialization plugin
    id("com.google.gms.google-services") // Google Services plugin for Firebase
    id("org.jetbrains.dokka") version "1.9.20" // Dokka for documentation generation
}

android {
    namespace = "tn.esprit.wayfinder"
    compileSdk = 36

    defaultConfig {
        applicationId = "tn.esprit.wayFinder"
        minSdk = 24
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

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Jetpack Compose dependencies
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.foundation) // Corrected line

    // Retrofit & Serialization
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // Useful for debugging

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // DataStore for caching
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // ExifInterface for image orientation handling
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    
    // Firebase Cloud Messaging
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    
    // ZXing for QR code generation
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Other dependencies
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.ads.mobile.sdk)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Create testClasses task alias for compatibility
// This task is required by some IDEs/build tools that expect standard Java project structure
// Register after Android plugin has been applied
afterEvaluate {
    if (tasks.findByName("testClasses") == null) {
        tasks.register("testClasses") {
            description = "Alias for compiling test classes in Android project"
            group = "verification"
            // Simple no-op task that always succeeds
            doLast {
                // Task completed successfully - Android projects use different test task names
            }
        }
    }
}

// Dokka Configuration for Documentation Generation
tasks.dokkaHtml.configure {
    outputDirectory.set(file("$rootDir/docs/android"))
    dokkaSourceSets.named("main") {
        includeNonPublic.set(false)
        skipEmptyPackages.set(true)
        skipDeprecated.set(true)
        reportUndocumented.set(false) // Don't fail on undocumented code
        jdkVersion.set(8)
    }
}

tasks.register("dokkaHtmlMultiModule") {
    description = "Generates documentation in HTML format for all modules"
    dependsOn(tasks.dokkaHtml)
}
