package com.example.qualcomm_teste_6.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.allcomtelecom.com"

    val instance: ApiService by lazy {
        // Configura o Interceptor para logs
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Exibe tudo: URL, headers, corpo da requisição/resposta
        }

        // Cria o cliente OkHttp com o Interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // Timeout de conexão
            .readTimeout(30, TimeUnit.SECONDS)    // Timeout de leitura
            .writeTimeout(30, TimeUnit.SECONDS)   // Timeout de escrita
            .build()

        // Cria o Retrofit com o cliente OkHttp
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Adiciona o cliente OkHttp ao Retrofit
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}