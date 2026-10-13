import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from a gitignored keystore.properties at the repo root, or from
// GLITCH_KEYSTORE_PATH / GLITCH_KEYSTORE_PASSWORD / GLITCH_KEY_ALIAS / GLITCH_KEY_PASSWORD.
// Each value is read from keystore.properties first, then from its environment variable.
// If nothing at all is configured, release builds are produced unsigned (CI does this).
// If signing is only partly configured, or storeFile does not exist, building a release
// package fails with a clear message instead of silently producing an unsigned bundle.
// The check is lazy, so debug builds, unit tests and lint still work meanwhile.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) keystorePropertiesFile.inputStream().use { load(it) }
}

// keystore.properties key to environment variable.
val signingKeys = linkedMapOf(
    "storeFile" to "GLITCH_KEYSTORE_PATH",
    "storePassword" to "GLITCH_KEYSTORE_PASSWORD",
    "keyAlias" to "GLITCH_KEY_ALIAS",
    "keyPassword" to "GLITCH_KEY_PASSWORD",
)

fun envValue(envKey: String): String? =
    providers.environmentVariable(envKey).orNull?.takeIf { it.isNotBlank() }

val signingValues: Map<String, String?> = signingKeys.mapValues { (propertyKey, envKey) ->
    keystoreProperties.getProperty(propertyKey)?.takeIf { it.isNotBlank() } ?: envValue(envKey)
}
val signingAttempted = keystorePropertiesFile.isFile || signingKeys.values.any { envValue(it) != null }
val missingSigningKeys = signingValues.filterValues { it == null }.keys
val hasReleaseSigning = missingSigningKeys.isEmpty()
val releaseStoreFile = signingValues["storeFile"]?.let { rootProject.file(it) }

val releaseSigningError: String? = when {
    !signingAttempted -> null
    !hasReleaseSigning -> "Release signing is partly configured; missing: " +
        missingSigningKeys.joinToString { "$it (${signingKeys.getValue(it)})" } +
        ". Fill in keystore.properties or unset the GLITCH_* variables."
    releaseStoreFile?.isFile != true -> "Release signing storeFile does not exist: " +
        "$releaseStoreFile. Fix storeFile in keystore.properties or GLITCH_KEYSTORE_PATH."
    else -> null
}

// Fail only when a task that produces a release package is about to run.
val releasePackagingTasks = setOf(
    ":app:packageRelease", ":app:signReleaseBundle", ":app:packageReleaseUniversalApk",
)
gradle.taskGraph.whenReady {
    val error = releaseSigningError ?: return@whenReady
    if (allTasks.any { it.path in releasePackagingTasks }) throw GradleException(error)
}

android {
    namespace = "com.benisalla.glitch"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.benisalla.glitch"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingValues["storePassword"]
                keyAlias = signingValues["keyAlias"]
                keyPassword = signingValues["keyPassword"]
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // AGP otherwise puts an encrypted dependency report, readable only by Google, into the
    // APK signing block. It is re-encrypted on every build, so APKs would not be reproducible
    // (the release workflow checks this). The AAB keeps it for Play Console.
    dependenciesInfo {
        includeInApk = false
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
