plugins {
    alias(libs.plugins.android.application)
    // Asegúrate de que no falten comas entre plugins
}

android {
    namespace = "com.example.gestordetareas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gestordetareas"
        minSdk = 24
        targetSdk = 35
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
            ) // Asegúrate de que los paréntesis estén balanceados
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Configuración adicional recomendada
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.volley)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gridlayout)
    implementation(libs.play.services.maps)
    implementation ("org.osmdroid:osmdroid-android:6.1.10")
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.android.volley:volley:1.2.1")
    implementation("commons-io:commons-io:2.11.0")
    implementation("androidx.activity:activity:1.6.0")
    implementation("androidx.fragment:fragment:1.5.5")

    // Lifecycle
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("com.google.firebase:firebase-crashlytics-buildtools:3.0.3")
    implementation(libs.annotation)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}