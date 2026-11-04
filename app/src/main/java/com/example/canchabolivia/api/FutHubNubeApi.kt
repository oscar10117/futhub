package com.example.canchabolivia.api

import com.example.canchabolivia.BuildConfig
import com.google.gson.JsonElement
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// Edge Functions de SQLite Cloud. Cada función se llama por su nombre:
//   POST <url><nombre_funcion>   con el encabezado  Authorization: Bearer <apikey>
// La URL y la API key salen de sqlitecloud.properties (ver app/build.gradle.kts).
// [GUIA DOCENTE - API 2] Puerta HTTP hacia las Edge Functions de SQLite Cloud.
// Nueva funcion remota: definirla en sqlitecloud/funciones/ y llamarla desde NubeRepository.
interface FutHubNubeApi {

    // [GUIA DOCENTE - POST] Envia JSON y un bearer token a la funcion indicada.
    // IMPORTANTE: API keys incrustadas en APK NO son secretas; requerir permisos en backend.
    @POST("{funcion}")
    suspend fun llamar(
        @Path("funcion") funcion: String,
        @Body cuerpo: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Authorization") autorizacion: String = "Bearer ${BuildConfig.SQLITECLOUD_APIKEY}"
    ): JsonElement

    companion object {
        // false si todavía no se llenó sqlitecloud.properties: la app sigue
        // funcionando con los datos de ejemplo, sin intentar conectarse.
        val configurada: Boolean =
            BuildConfig.SQLITECLOUD_URL.isNotBlank() && BuildConfig.SQLITECLOUD_APIKEY.isNotBlank()

        val instancia: FutHubNubeApi by lazy {
            val base = BuildConfig.SQLITECLOUD_URL.let { if (it.endsWith("/")) it else "$it/" }
            Retrofit.Builder()
                .baseUrl(base)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FutHubNubeApi::class.java)
        }
    }
}
