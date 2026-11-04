plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

evaluationDependsOn(":app")
val phoneAndroid = project(":app").extensions.getByType<com.android.build.api.dsl.ApplicationExtension>()

android {
    namespace = "com.repforge.app.wear"
    compileSdk { version = release(36) { minorApiLevel = 1 } }
    defaultConfig {
        // Data Layer requires matching phone identity and signer.
        applicationId = "com.TrainPilot.com"
        minSdk = 30
        targetSdk = 36
        versionCode = 2753
        versionName = "0.1.0-beta"
    }
    signingConfigs {
        create("phoneDebug") { initWith(phoneAndroid.signingConfigs.getByName("debugConfig")) }
        create("phoneRelease") { initWith(phoneAndroid.signingConfigs.getByName("release")) }
    }
    buildTypes {
        debug { signingConfig = signingConfigs.getByName("phoneDebug") }
        release {
            signingConfig = signingConfigs.getByName("phoneRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Release optimizations, explicitly a test artifact signed with the existing phone debug key.
        create("optimized") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("phoneDebug")
            matchingFallbacks += listOf("release")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.fragment:fragment:1.8.6")
    implementation("androidx.health:health-services-client:1.1.0")
    implementation(libs.guava)
    implementation("androidx.wear.tiles:tiles:1.5.0")
    implementation("androidx.wear.protolayout:protolayout:1.3.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")
    implementation(libs.androidx.wear.remote.interactions)
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.compose:compose-material:1.5.0")
    implementation("androidx.wear.compose:compose-material3:1.5.0")
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
}
