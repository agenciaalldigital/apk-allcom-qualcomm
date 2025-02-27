package com.example.qualcomm_teste_6.network

data class CreateRequestPostLocation(
    val lat: String,       // Campo "lat"
    val long: String,      // Campo "long"
    val country: String,   // Campo "country"
    val data: String,       // Campo "data"
    val batteryPercentege: Int, // porcentagem da bateria
    val serial_number: String, //IMEI
    val device_name: String, //MARCA
    val model_device_name: String, //MODELO
    val ip_address_networking: String,
    val ip_address_public_networking: String
)
