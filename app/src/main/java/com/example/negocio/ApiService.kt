package com.example.negocio

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

data class IneigiData(
    val pob_adulta: List<Int>,
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
    val negocio_tipo: List<String>,
    val lat: List<Double>,
    val lng: List<Double>,
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

data class LugarCercano(
    val lat: List<Double>,
    val lng: List<Double>,
    val tipo: List<String>
)

data class LugaresCercanosResponse(
    val total: List<Int>,
    val lugares: List<LugarCercano>
)

interface ApiService {

    @GET("ping")
    suspend fun ping(): Response<PingResponse>

    @FormUrlEncoded
    @POST("predecir")
    suspend fun predecir(
        @Field("lat")   lat: Double,
        @Field("lng")   lng: Double,
        @Field("tipo")  tipo: String,
        @Field("radio") radio: Int = 1000
    ): Response<PrediccionResponse>

    @FormUrlEncoded
    @POST("lugares_cercanos")
    suspend fun getLugaresCercanos(
        @Field("lat")   lat: Double,
        @Field("lng")   lng: Double,
        @Field("tipo")  tipo: String,
        @Field("radio") radio: Int = 1000
    ): Response<LugaresCercanosResponse>

    @FormUrlEncoded
    @POST("mejor_punto")
    suspend fun getMejorPunto(
        @Field("lat")   lat: Double,
        @Field("lng")   lng: Double,
        @Field("tipo")  tipo: String,
        @Field("radio") radio: Int = 1000
    ): Response<List<PrediccionResponse>>
}

object RetrofitClient {

    private const val BASE_URL = "https://geobiz-api-latest-1.onrender.com/"

    fun getApi(context: android.content.Context): ApiService {
        val certificatePinner = CertificatePinner.Builder()
            .add("geobiz-api-latest-1.onrender.com", "sha256/T4eoRdbfIYF3G9IOGamqR3Vgye2bNLHQTSCOY8u3y5w=")
            .add("geobiz-api-latest-1.onrender.com", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
            .add("geobiz-api-latest-1.onrender.com", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .certificatePinner(certificatePinner)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
