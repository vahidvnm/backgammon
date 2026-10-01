plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dara.backgammon"
    compileSdk = 35

    signingConfigs.create("stableDebug") {
        storeFile = file("ci-debug.p12")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
        storeType = "PKCS12"
    }

    val releaseStoreFile = System.getenv("ANDROID_KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    if (!releaseStoreFile.isNullOrBlank() && !releaseStorePassword.isNullOrBlank() && !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()) {
        signingConfigs.create("release") {
            storeFile = file(releaseStoreFile)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }
    defaultConfig {
        applicationId = "com.dara.backgammon"
        minSdk = 24
        targetSdk = 35
        versionCode = 13
        versionName = "1.0.0-rc13"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += "store"
    productFlavors {
        create("myket") {
            dimension = "store"
            buildConfigField("String", "STORE_NAME", "\"مایکت\"")
            buildConfigField("String", "STORE_PACKAGE", "\"ir.mservices.market\"")
            buildConfigField("String", "DETAILS_URI", "\"myket://details?id=com.dara.backgammon\"")
            buildConfigField("String", "RATE_URI", "\"myket://comment?id=com.dara.backgammon\"")
            buildConfigField("String", "UPDATE_URI", "\"myket://check-update?id=com.dara.backgammon\"")
        }
        create("bazaar") {
            dimension = "store"
            buildConfigField("String", "STORE_NAME", "\"کافه‌بازار\"")
            buildConfigField("String", "STORE_PACKAGE", "\"com.farsitel.bazaar\"")
            buildConfigField("String", "DETAILS_URI", "\"bazaar://details?id=com.dara.backgammon\"")
            buildConfigField("String", "RATE_URI", "\"bazaar://details?id=com.dara.backgammon\"")
            buildConfigField("String", "UPDATE_URI", "\"bazaar://details?id=com.dara.backgammon\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
