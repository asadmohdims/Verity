plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.verity.platform"
    compileSdk {
        version = release(36)
    }
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
        getByName("androidTest") {
            // "schemas" holds Room's exported per-version schema JSON (see the ksp block below) —
            // MigrationTestHelper reads a prior version's schema from here to build a real
            // pre-migration database in DocumentEntityMigrationTest.
            assets.srcDirs("src/androidTest/assets", "schemas")
        }
    }
    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(project(":core"))
    implementation(project(":feature"))
}

ksp {
    // Exports each @Database version's schema as JSON under schemas/, so
    // MigrationTestHelper (in androidTest) can build a real pre-migration database instead of
    // hand-writing CREATE TABLE statements per version.
    arg("room.schemaLocation", "$projectDir/schemas")
}