plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("android-jacoco-convention")
    id("android-spotless-convention")
    id("android-detekt-convention")
}

android {
    namespace = "com.wedgess.luas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wedgess.luas"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
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
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.bundles.sqldelight)

    implementation(libs.timber)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.javalite)

    implementation(libs.bundles.ktor)

    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.maplibre.android.sdk)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.core.test.ktx)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

sqldelight {
    databases {
        create("LuasDatabase") {
            packageName.set("com.wedgess.luas.data")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
        }
    }
}
