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

        // --- INICIO DE LA SECCIÓN PARA LA API KEY (REINTRODUCIENDO local.properties) ---

        // 1. Inicializa Properties
        val localProperties = Properties()

        // 2. Define la ruta al archivo local.properties
        val localPropertiesFile = rootProject.file("local.properties") // Busca en la raíz del proyecto

        // 3. Declara una variable para almacenar la clave cargada (usa 'var' si la asignas en diferentes ramas)
        var apiKeyCargada = "API_KEY_POR_DEFECTO_SI_FALLA_LA_CARGA" // Un valor por defecto para saber si algo falló

        // 4. Verifica si el archivo existe y cárgalo
        if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
            try {
                // Usa .inputStream().use { ... } para asegurar que el stream se cierre
                localPropertiesFile.inputStream().use { inputStream ->
                    localProperties.load(inputStream)
                }

                // 5. Obtén la propiedad del archivo.
                //    ¡EL NOMBRE AQUÍ DEBE SER EXACTAMENTE EL MISMO QUE EN local.properties!
                apiKeyCargada = localProperties.getProperty("GOOGLE_MAPS_API_KEY", "LLAVE_NO_ENCONTRADA_EN_PROPS")
                // El segundo argumento es un valor por defecto si la propiedad no se encuentra

                println("INFO: API Key encontrada en local.properties y cargada.")

            } catch (e: java.io.IOException) {
                println("ADVERTENCIA: No se pudo leer local.properties: ${e.message}")
                apiKeyCargada = "ERROR_AL_LEER_PROPERTIES" // Indica un error de lectura
            }
        } else {
            println("ADVERTENCIA: El archivo local.properties no fue encontrado en la raíz del proyecto.")
            apiKeyCargada = "LOCAL_PROPERTIES_NO_ENCONTRADO" // Indica que el archivo no existe
        }

        // 6. Usa la variable cargada (apiKeyCargada) para el placeholder del manifiesto.
        //    El nombre del placeholder "GOOGLE_MAPS_API_KEY" debe coincidir con ${...} en AndroidManifest.xml
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = apiKeyCargada

        // 7. Define también el buildConfigField para acceder desde el código (opcional pero buena práctica).
        //    Puedes usar un nombre diferente para el campo de BuildConfig si quieres.
        buildConfigField("String", "MY_APP_MAPS_API_KEY", "\"${apiKeyCargada}\"")
        //    (En tu código Kotlin/Java, accederías a esto como BuildConfig.MY_APP_MAPS_API_KEY)

        println("INFO: Valor final asignado a manifestPlaceholders[GOOGLE_MAPS_API_KEY]: $apiKeyCargada")
        println("INFO: Valor final asignado a buildConfigField[MY_APP_MAPS_API_KEY]: $apiKeyCargada")
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

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.glance:glance:1.2.0-alpha01")
    implementation("androidx.glance:glance-appwidget:1.2.0-alpha01")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")


}