import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}
// Conexión a SQLite Cloud: se lee de sqlitecloud.properties (en la raíz del
// proyecto, NO se sube a Git). Si no existe, la app usa los datos de ejemplo.
// [GUIA DOCENTE - CONFIG DB] Lee la configuracion LOCAL para SQLite Cloud.
// Nunca subir sqlitecloud.properties con credenciales.
val sqliteCloud = Properties().apply {
    val archivo = rootProject.file("sqlitecloud.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "com.example.canchabolivia"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.canchabolivia"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SQLITECLOUD_URL", "\"${sqliteCloud.getProperty("url", "").trim()}\"")
        buildConfigField("String", "SQLITECLOUD_APIKEY", "\"${sqliteCloud.getProperty("apikey", "").trim()}\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
// [GUIA DOCENTE - DEPENDENCIAS] Librerias de Retrofit/Gson (APIs), Room y Firebase.
// Nueva API: anadir dependencia aqui solo si no esta disponible en el proyecto.
dependencies {
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // Retrofit: consumo de la API de Open-Meteo
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room: caché local del último clima (Entity + DAO + Database)
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // Firebase: el BoM fija versiones compatibles entre sus librerías
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
