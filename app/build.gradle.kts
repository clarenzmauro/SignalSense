plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services) // Firebase plugin
}

android {
    namespace = "com.example.mobilecomputing"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mobilecomputing"
        minSdk = 24
        targetSdk = 34
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase Authentication
    implementation(libs.firebase.auth)

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation(libs.firebase.firestore)

    // MPAndroidChart for statistics
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // OSMDroid for OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.16")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
