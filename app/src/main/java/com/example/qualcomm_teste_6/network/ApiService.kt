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

    @POST("/api:Wf0orOZQ/allcomdevicesaware")
    fun createItem(@Body request: CreateRequestPostLocation): Call<CreateRequestResponse>
}