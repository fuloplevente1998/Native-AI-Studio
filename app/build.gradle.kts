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

// The normal local and persistent-signing test task still runs every test.
// Only full CI's phone matrix opts into disjoint exact class filters.
val phoneTestShard = providers.gradleProperty("trainpilotPhoneTestShard").orNull
require(phoneTestShard == null || phoneTestShard in setOf("a", "b")) {
  "trainpilotPhoneTestShard must be a or b"
}
val phoneShardBClasses = if (phoneTestShard != null) {
  providers.fileContents(rootProject.layout.projectDirectory.file("scripts/ci_phone_test_shard_b.txt"))
    .asText.get().lineSequence().map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }.toList().also { names ->
      require(names.isNotEmpty() && names.size == names.toSet().size) { "Invalid phone shard B manifest" }
      require(names.all { it.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")) }) {
        "Phone shard B must contain exact fully qualified class names"
      }
    }
} else emptyList()

android {
  namespace = "com.repforge.app"
  compileSdk { version = release(37) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.TrainPilot.com"
    manifestPlaceholders["TRAINPILOT_MAPS_API_KEY"] = mapsApiKey
    minSdk = 26
    targetSdk = 36
    versionCode = 282100 // Play scheme: release 2821, mobile suffix 00
    versionName = "1.5.8"

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
        if (phoneTestShard == "a") phoneShardBClasses.forEach { test.filter.excludeTestsMatching(it) }
        if (phoneTestShard == "b") phoneShardBClasses.forEach { test.filter.includeTestsMatching(it) }
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
  // Compose BOM 2026.11.01 (1.13) requires compileSdk 37.1; unchanged minSdk/targetSdk.
  implementation(platform(libs.androidx.compose.bom.kyant))
  implementation(libs.kyant.backdrop)
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
  androidTestImplementation(platform(libs.androidx.compose.bom.kyant))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
