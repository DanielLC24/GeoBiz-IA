package com.example.negocio

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.GET

// ── Modelos de respuesta ──────────────────────────────────────────
data class IneigiData(
    val pob_adulta: List<Int>,
    val pct_internet: List<Double>,
    val pct_posbas: List<Double>,
    val indice_socio: List<Double>
)

data class OsmData(
    val cafes: List<Int>,
    val bares: List<Int>,
    val fast_food: List<Int>,
    val paradas_bus: List<Int>,
    val bancos: List<Int>,
    val supermercados: List<Int>,
    val restaurantes: List<Int>,
    val competencia_directa: List<Int>
)

data class PrediccionResponse(
    val lat: List<Double>,
    val lng: List<Double>,
    val radio_metros: List<Int>,
    val score_final: List<Double>,
    val score_osm: List<Double>,
    val score_inegi: List<Double>,
    val recomendacion: List<String>,
    val inegi: IneigiData,
    val osm: OsmData
)

data class PingResponse(
    val status: List<String>,
    val mensaje: List<String>
)

// ── Interface de la API ───────────────────────────────────────────
interface ApiService {

    @GET("ping")
    suspend fun ping(): Response<PingResponse>

    @FormUrlEncoded
    @POST("predecir")
    suspend fun predecir(
        @Field("lat")   lat: Double,
        @Field("lng")   lng: Double,
        @Field("radio") radio: Int = 1000
    ): Response<PrediccionResponse>
}

// ── Cliente Retrofit ──────────────────────────────────────────────
object RetrofitClient {

    // Cambia esta IP por la de tu computadora en la red local
    // Para saber tu IP: en Windows → cmd → ipconfig → IPv4
    private const val BASE_URL = "http://192.168.100.17:8000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}