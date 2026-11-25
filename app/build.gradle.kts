import java.util.Properties
import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// Local/AI Studio builds may use ignored properties; Actions supplies the same key via env.
// Keep credentials out of source and do not alter the existing signing configurations.
val localMapsProperties = Properties().apply {
    listOf("local.properties", "secrets.properties", ".env").forEach { name ->
        val source = rootProject.file(name)
        if (source.isFile) source.inputStream().use { load(it) }
    }
}
val mapsApiKey = sequenceOf(
    providers.environmentVariable("TRAINPILOT_MAPS_API_KEY").orNull,
    providers.gradleProperty("TRAINPILOT_MAPS_API_KEY").orNull,
    localMapsProperties.getProperty("TRAINPILOT_MAPS_API_KEY"),
    localMapsProperties.getProperty("MAPS_API_KEY")
).firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

// [Test APK only] -PtrainpilotTestVariant=<name> (isolated CI job) turns the DEBUG build into a separate app:
// own package suffix, launcher label and deep-link scheme. Without it debug/release are unchanged.
val testVariant = providers.gradleProperty("trainpilotTestVariant").orNull?.takeIf { it.matches(Regex("[a-z0-9]{1,12}")) }

android {
  namespace = "com.repforge.app"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.TrainPilot.com"
    manifestPlaceholders["TRAINPILOT_MAPS_API_KEY"] = mapsApiKey
    manifestPlaceholders["trainpilotAppLabel"] = "@string/app_name"
    manifestPlaceholders["trainpilotWearScheme"] = "trainpilot"
    minSdk = 26
    targetSdk = 36
    versionCode = 280700 // Play scheme: release 2807, mobile suffix 00
    versionName = "1.5.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "DEVELOPER_NAME", "\"FulTechStudios\"")
    buildConfigField("String", "SUPPORT_EMAIL", "\"paszia1998@gmail.com\"")
    buildConfigField("String", "PRIVACY_POLICY_URL", "\"https://fuloplevente1998.github.io/TrainPilot.io/privacy.html\"")
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      if (testVariant != null) {
        applicationIdSuffix = ".lg$testVariant"
        versionNameSuffix = "-lg-$testVariant"
        manifestPlaceholders["trainpilotAppLabel"] = "TrainPilot LG $testVariant"
        manifestPlaceholders["trainpilotWearScheme"] = "trainpilotlg$testVariant"
      }
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
      all { test ->
        // Two isolated Robolectric JVMs: shorter wall time on 4-vCPU CI runners.
        test.maxParallelForks = 2
        test.maxHeapSize = "3g"
      }
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

dependencies {
  implementation(project(":shared"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.paging.runtime.ktx)
  implementation(libs.androidx.paging.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.paging)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.play.services.wearable)
  implementation(libs.play.services.maps)
  implementation("com.google.maps.android:maps-compose:4.3.0")
  implementation(libs.play.services.auth)
  implementation(libs.androidx.wear.remote.interactions)
  implementation(libs.guava)
  implementation(libs.androidx.exifinterface)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.gson)
  implementation(files("libs/samsung-health-data-api-1.1.0.aar"))
  // Stable cross-version Health Connect client (APK on Android 9-13, platform on 14+).
  implementation("androidx.health.connect:connect-client:1.1.0")

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
