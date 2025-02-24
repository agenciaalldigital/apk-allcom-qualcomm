package com.example.qualcomm_teste_6.network

data class CreateRequestResponse(
    val id: Long,
    val created_at: Long,
    val lat: String,
    val long: String,
    val country: String,
    val data: String
)
