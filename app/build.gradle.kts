import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("org.jetbrains.kotlin.plugin.serialization") }

val defaultSigningProperties = file("${System.getProperty("user.home")}/.openrouter-credits-widget/keystore.properties")
val signingPropertiesFile = System.getenv("OPENROUTER_SIGNING_PROPERTIES")?.let(::file) ?: defaultSigningProperties
val signingProperties = Properties()
val hasReleaseSigning = signingPropertiesFile.isFile
if (hasReleaseSigning) signingPropertiesFile.inputStream().use(signingProperties::load)

if (gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) } && !hasReleaseSigning) {
    throw GradleException("Release signing properties not found: $signingPropertiesFile")
}

android { namespace = "ai.openrouter.creditswidget"
    compileSdk { version = release(36) { minorApiLevel = 1 } }
    defaultConfig { applicationId = "ai.openrouter.creditswidget"; minSdk = 26; targetSdk = 36; versionCode = 3; versionName = "0.1.2" }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.10.2")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.21")
}
