import java.util.Properties // Import para Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.revisit"
    compileSdk = 35

    buildFeatures {
        buildConfig = true  // <--- ¡ESTA ES LA LÍNEA CLAVE!
        compose = true      // Si usas Compose, mantenla
        // ... otras features que puedas tener ...
    }

    defaultConfig {
        applicationId = "com.isaiasmonroy.revisit"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- INICIO DE LA SECCIÓN PARA LA API KEY ---

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        var apiKeyCargada = "API_KEY_DEFAULT_FALLBACK" // Valor por defecto en caso de fallo total
        if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
            try {
                localPropertiesFile.inputStream().use { inputStream ->
                    localProperties.load(inputStream)
                }
                apiKeyCargada = localProperties.getProperty("GOOGLE_MAPS_API_KEY", apiKeyCargada)
            } catch (_: java.io.IOException) {
            }
        }
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = apiKeyCargada
        buildConfigField("String", "MY_APP_MAPS_API_KEY", "\"${apiKeyCargada}\"")

// --- FIN DE LA SECCIÓN PARA LA API KEY ---

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
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0") // O la versión que decidas (2.8.0 o 2.9.0)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0") // O la versión que decidas
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0") // Asumiendo que quieres la 2.9.0 o ajusta según necesites. Esto reemplaza runtime-ktx para proyectos Compose.

    // Core KTX (desde libs.versions.toml)
    implementation(libs.androidx.core.ktx)

    // Compose (gestionado por BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Esta tomará la versión del BOM. Asegúrate que sea compatible o >= 1.3.1

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.9.0") // Asegúrate que esta versión sea la deseada

    // Activity Compose (desde libs.versions.toml)
    implementation(libs.androidx.activity.compose)

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0") // Solo una vez

    // Material Icons
    implementation("androidx.compose.material:material-icons-core:1.5.4") // Verifica si hay versiones más nuevas si es necesario
    implementation("androidx.compose.material:material-icons-extended:1.5.4") // Verifica si hay versiones más nuevas si es necesario

    // Glance (AppWidgets)
    implementation("androidx.glance:glance:1.2.0-alpha01")
    implementation("androidx.glance:glance-appwidget:1.2.0-alpha01")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.3") // Solo una vez, revisa la última versión
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Revisa la última versión
    implementation("com.google.android.gms:play-services-location:21.3.0") // Revisa la última versión

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // LiveData con Compose (si aún la usas activamente, si no, considera removerla si te has movido completamente a StateFlow)
    implementation("androidx.compose.runtime:runtime-livedata:1.7.0-beta01")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // BOM para pruebas de Compose
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Take photo
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Dialing phone numbers
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.36")
}
