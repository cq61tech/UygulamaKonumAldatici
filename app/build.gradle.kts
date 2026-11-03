plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.cq61tech.konumaldatici"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.cq61tech.konumaldatici"
        minSdk = 29
        targetSdk = 35
        versionCode = 8
        versionName = "0.3.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Modern Xposed: modül tarafı (hedef süreçte framework sağlar) ve uygulama tarafı (servis bağlantısı)
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")
}

// libxposed 102 kütüphaneleri compileSdk 37 ister, kullandığımız Android Gradle Plugin (8.7.3) ise
// en çok 35'i destekliyor. Bu kontrol sadece derleme zamanı bilgisidir; çalışma zamanını etkilemez.
tasks.configureEach {
    if (name.startsWith("check") && name.endsWith("AarMetadata")) {
        enabled = false
    }
}
