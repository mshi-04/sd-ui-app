plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
}

val apiBaseUrl =
  providers.gradleProperty("API_BASE_URL").orElse("https://example.com/api/").map { url ->
    if (url.endsWith('/')) url else "$url/"
  }
val apiScope =
  providers.gradleProperty("API_SCOPE")
    .orElse("api://33333333-3333-4333-8333-333333333333/UiSchema.Read")
val devOid = providers.gradleProperty("DEV_OID").orElse("")
val devRoles = providers.gradleProperty("DEV_ROLES").orElse("")

android {
    namespace = "com.example.sdui"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.sdui"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
        buildConfigField("String", "API_SCOPE", "\"${apiScope.get()}\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEV_OID", "\"${devOid.get()}\"")
            buildConfigField("String", "DEV_ROLES", "\"${devRoles.get()}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Dependency injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Network
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx.serialization)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)

  // Authentication
  implementation(libs.msal)

  // Local tests
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockk)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.turbine)
  testRuntimeOnly(libs.junit.platform.launcher)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

}
