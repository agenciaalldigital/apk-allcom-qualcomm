package com.example.qualcomm_teste_6

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.skyhookwireless.wps.IWPS
import com.skyhookwireless.wps.WPSContinuation
import com.skyhookwireless.wps.WPSLocation
import com.skyhookwireless.wps.WPSLocationCallback
import com.skyhookwireless.wps.WPSReturnCode
import com.skyhookwireless.wps.WPSStreetAddressLookup
import com.skyhookwireless.wps.XPS
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.qualcomm_teste_6.network.RetrofitClient
import com.example.qualcomm_teste_6.network.CreateRequestPostLocation

class LocationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private lateinit var xps: IWPS

    override fun doWork(): Result {
        xps = XPS(applicationContext)
        try {
            xps.setKey("eJwNwcENACEIBMC3xZC4KiJfxG3qcr3rDArqA-iy8lEj1A-lOU0MJ2Vwh3hfiZGT8PZfGIILYA")
        } catch (e: IllegalArgumentException) {
            Log.e("LocationWorker", "Erro ao configurar a chave: ${e.message}")
            return Result.failure()
        }

        // Obtém a localização
        determineLocation(xps)

        return Result.success()
    }

    private fun determineLocation(xps: IWPS) {

        xps.getLocation(
            null,
            WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP,
            false,
            object : WPSLocationCallback {
                override fun handleWPSLocation(location: WPSLocation) {
                    // Extrai os dados da localização
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val precision = location.hpe
                    val country = location.streetAddress.toString() ?: "Brasil"
                    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    println("latitude")
                    println(latitude)

                    // Salva o resultado e a hora da última busca
                    saveLocationResult("Latitude: $latitude, Longitude: $longitude, Precisão: $precision, País: $country")
                    saveLastUpdateTime()

                    // Faz a requisição POST
                    makePostRequest(latitude.toString(), longitude.toString(), country, timestamp)
                }

                override fun done() {
                    // Handle completion if needed
                }

                override fun handleError(returnCode: WPSReturnCode): WPSContinuation {
                    Log.e("LocationWorker", "Erro na localização: $returnCode")
                    return WPSContinuation.WPS_CONTINUE
                }
            }
        )
    }

    private fun saveLocationResult(result: String) {
        val sharedPref = applicationContext.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("lastLocationResult", result)
            apply()
        }
    }

    private fun saveLastUpdateTime() {
        val sharedPref = applicationContext.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("lastUpdateTime", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
            apply()
        }
    }

    private fun makePostRequest(lat: String, long: String, country: String, data: String) {
        val apiService = RetrofitClient.instance
        val request = CreateRequestPostLocation(lat, long, country, data)

        Thread {
            try {
                val response = apiService.createItem(request).execute()
                if (response.isSuccessful) {
                    Log.d("LocationWorker", "Item criado com sucesso! ID: ${response.body()?.id}")
                } else {
                    Log.e("LocationWorker", "Falha ao criar item: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("LocationWorker", "Erro na requisição: ${e.message}")
            }
        }.start()
    }
}