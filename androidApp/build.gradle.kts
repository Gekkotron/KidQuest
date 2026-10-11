import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(name)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.gekkotron.kidquest"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.gekkotron.kidquest"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        create("release") {
            val keystorePath = secret("KIDQUEST_KEYSTORE_PATH")
            if (keystorePath != null) {
                val storePwd = secret("KIDQUEST_KEYSTORE_PASSWORD")
                    ?: secret("KIDQUEST_KEY_PASSWORD").orEmpty()
                val keyPwd = secret("KIDQUEST_KEY_PASSWORD")
                    ?: secret("KIDQUEST_KEYSTORE_PASSWORD").orEmpty()
                storeFile = file(keystorePath)
                storePassword = storePwd
                keyAlias = secret("KIDQUEST_KEY_ALIAS")
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        release {
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
}
