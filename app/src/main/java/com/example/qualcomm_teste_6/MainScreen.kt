
package com.example.qualcomm_teste_6

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.qualcomm_teste_6.network.CreateRequestPostLocation
import com.example.qualcomm_teste_6.network.RetrofitClient
import com.example.qualcomm_teste_6.ui.theme.roxoAllcom
import com.example.qualcomm_teste_6.ui.theme.warning
import com.skyhookwireless.wps.IWPS
import com.skyhookwireless.wps.WPSContinuation
import com.skyhookwireless.wps.WPSLocation
import com.skyhookwireless.wps.WPSLocationCallback
import com.skyhookwireless.wps.WPSPeriodicLocationCallback
import com.skyhookwireless.wps.WPSReturnCode
import com.skyhookwireless.wps.WPSStreetAddressLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(xps: IWPS) {
    var locationText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isPeriodicUpdateEnabled by remember { mutableStateOf(false) }
    var responseText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val apiService = RetrofitClient.instance
    val request = CreateRequestPostLocation("-41.0202023091", "-12.123912309", "Brasil", "2025-02-20")

    fun makePostRequest() {
        scope.launch {
            try {
                responseText = "Fazendo requisição..." // Atualiza o texto para mostrar que a requisição está em andamento
                val response = withContext(Dispatchers.IO) {
                    apiService.createItem(request).execute()
                }

                if (response.isSuccessful) {
                    val createdResponse = response.body()
                    if(createdResponse != null){
                        responseText = "Item criado com sucesso! ID: ${createdResponse.id}"
                        Log.d("MainScreen", "Item criado com sucesso!")
                    }else{
                        responseText = "ITEM CRIADO: Sem resposta após a criação"
                        Log.e("MainScreen", "Resposta inválida do servidor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    responseText = "Falha ao criar item: $errorBody"
                    Log.e("MainScreen", "Falha ao criar item: $errorBody")
                }
            } catch (e: Exception) {
                responseText = "Erro na requisição: ${e}"
                Log.e("MainScreen", "Erro na requisição: ${e.message}", e)
            }
        }
    }

    LaunchedEffect(isPeriodicUpdateEnabled) {
        while (isPeriodicUpdateEnabled) {
            determineLocation(xps) { result ->
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                locationText = "Ultima verificação: $timestamp\n$result"
            }
            delay(15 * 60 * 1000)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(roxoAllcom),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ALLCOM TELECOM",
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = roxoAllcom
                        ),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                determineLocation(xps) { result ->
                                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                    locationText = "[$timestamp]\n$result"
                                    isLoading = false
                                }
                            }
                            isLoading = true
                        }
                    ) {
                        Text("Find my location")
                    }
                }
                Text(text = responseText)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = locationText)
                println(locationText)
            }
        }
    }
}

private suspend fun determineLocation(xps: IWPS, callback: (String) -> Unit) {
    println("chamando esse")
    xps.getLocation(
        null,
        WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP,
        false,
        object : WPSLocationCallback {
            override fun handleWPSLocation(location: WPSLocation) {

                val streetAddress = if(location.hasStreetAddress()){
                    location.streetAddress
                }else{
                    null
                }

                println("velocidade: ${location.speed}")

                val result = String.format(
                    Locale.ROOT,
                    "%.7f %.7f +/-%dm\n\n%s\n\n%s",
                    location.latitude,
                    location.longitude,
                    location.hpe,
                    streetAddress?.city ?: "Sem cidade",
                    streetAddress?.region ?: "Sem regiao",
                    streetAddress?.stateName ?: "Sem estado",
                    streetAddress?.countryName ?: "Sem country name",


                    if (location.hasTimeZone()) location.timeZone else "No timezone",
                    if (location.hasStreetAddress()) location.streetAddress else "No address"
                )
                callback(result)
            }

            override fun done() {
                // Handle completion if needed
            }

            override fun handleError(returnCode: WPSReturnCode): WPSContinuation {
                callback(returnCode.toString())
                return WPSContinuation.WPS_CONTINUE
            }
        }
    )
}