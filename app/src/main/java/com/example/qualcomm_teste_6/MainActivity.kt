package com.example.qualcomm_teste_6

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.qualcomm_teste_6.ui.theme.QUALCOMMTESTE6Theme
import com.skyhookwireless.wps.WPS
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // Estado para armazenar o último resultado da localização
    private var lastLocationResult by mutableStateOf("Nenhum resultado ainda.")
    private var lastUpdateTime by mutableStateOf("Nenhuma busca realizada.")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configura a URL do servidor
        WPS.setServerUrl("https://int-blue-aio-lb.skyhook.com/wps2")

        // Solicita permissões de localização
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0
        )

        // Agenda o Worker para rodar periodicamente (mesmo com intervalo mínimo de 15 minutos)
        scheduleLocationWorker()

        // Lê o último resultado da localização e a hora da última busca
        readLocationResult()

        // Define a interface do Compose
        setContent {
            QUALCOMMTESTE6Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationScreen(
                        lastLocationResult = lastLocationResult,
                        lastUpdateTime = lastUpdateTime,
                        onForceUpdate = { forceLocationUpdate() }
                        )

                }
            }
        }
    }

    private fun scheduleLocationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requer conexão com a internet
            .build()

        // Cria uma requisição periódica (intervalo mínimo de 15 minutos)
        val workRequest = PeriodicWorkRequest.Builder(
            LocationWorker::class.java,
            15, // Intervalo mínimo permitido
            TimeUnit.MINUTES
        ).setConstraints(constraints)
            .build()

        // Enfileira o Worker
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LocationWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun forceLocationUpdate() {
        val workRequest = OneTimeWorkRequest.Builder(LocationWorker::class.java).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    // Função para ler o último resultado da localização e a hora da última busca
    private fun readLocationResult() {
        val sharedPref = getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        lastLocationResult = sharedPref.getString("lastLocationResult", "Nenhum resultado ainda.") ?: "Nenhum resultado ainda."
        lastUpdateTime = sharedPref.getString("lastUpdateTime", "Nenhuma busca realizada.") ?: "Nenhuma busca realizada."
    }

    // Função para atualizar o último resultado da localização e a hora da última busca
    fun updateLocationResult(result: String) {
        lastLocationResult = result
        lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
}

@Composable
fun LocationScreen(
    lastLocationResult: String,
    lastUpdateTime: String,
    onForceUpdate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Nome da Allcomtelecom
        Text(
            text = "Allcomtelecom",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hora atual
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Text(
            text = "Hora atual: $currentTime",
            color = Color.Black,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Último resultado da localização
        Text(
            text = "Último resultado:\n$lastLocationResult",
            color = Color.Black,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        // Hora da última busca
        Text(
            text = "Última busca: $lastUpdateTime",
            color = Color.Black,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

//        Button(
//        onClick = onForceUpdate, // Chama a função passada como parâmetro
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 32.dp)
//        ) {
//        Text(text = "Forçar Atualização")
//    }


    }
}