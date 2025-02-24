package com.example.qualcomm_teste_6.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers(
        "accept: application/json",
        "Content-Type: application/json"
    )
    @POST("/api:U2HVnX-r/app_qualcomm") // Endpoint raiz
    fun createItem(@Body request: CreateRequestPostLocation): Call<CreateRequestResponse>
}