package com.example.qualcomm_teste_6

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.qualcomm_teste_6.service.LocationForegroundService
import com.example.qualcomm_teste_6.ui.theme.QUALCOMMTESTE6Theme
import com.example.qualcomm_teste_6.utils.DeviceInfoUtils
import com.example.qualcomm_teste_6.utils.XpsSingleton
import kotlinx.coroutines.*
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val PERMISSION_REQUEST_CODE = 123
    private val utils = DeviceInfoUtils

    private var loadingJob: Job? = null
    private var lastLocationResult by mutableStateOf("Nenhum resultado ainda.")
    private var lastUpdateTime by mutableStateOf("Nenhuma busca realizada.")
    private var isLoading by mutableStateOf(false)
    private var batteryPercentage by mutableStateOf("N/A")
    private var wifiSignalStrength by mutableStateOf("N/A")
    private var mobileSignalStrength by mutableStateOf("N/A")
    private var imeiDevide by mutableStateOf("N/A")
    private var brandDevice by mutableStateOf("N/A")
    private var modelDevice by mutableStateOf("N/A")
    private var ipLocalidade by mutableStateOf("Sem Ip Location")

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Se todas as permissões foram concedidas
            if (permissions.all { it.value }) {
                updateDeviceInfo()
                forceLocationUpdate()
            }
        }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        )

        val permissionsToRequestArray = permissionsToRequest.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequestArray.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequestArray)
        } else {
            // Se já temos todas as permissões, atualiza as informações
            updateDeviceInfo()
        }
    }

    private fun updateDeviceInfo() {
        try {
            batteryPercentage = utils.getBatteryPercentage(this).toString()
            wifiSignalStrength = utils.getWifiSignalStrength(this).toString()
            mobileSignalStrength = utils.getMobileSignalStrength(this).toString()

            imeiDevide = utils.getDeviceId(this)
                ?: Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                        ?: "Não disponível"

            brandDevice = utils.getDeviceManufacturer()
            modelDevice = utils.getDeviceModel()

            Log.d("DeviceInfo", "Informações atualizadas com sucesso")
        } catch (e: Exception) {
            Log.e("DeviceInfo", "Erro ao atualizar informações: ${e.message}")
        }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            readLocationResult() // Atualiza os dados na tela
            isLoading = false // Desativa o estado de loading
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        XpsSingleton.initialize(this)

        requestPermissions()

        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                1
            )
        }

        // Registrar o receiver para atualizações de localização
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                updateReceiver,
                IntentFilter("com.example.qualcomm_teste_6.LOCATION_UPDATE"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                updateReceiver,
                IntentFilter("com.example.qualcomm_teste_6.LOCATION_UPDATE")
            )
        }

        // Registrar também com LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).registerReceiver(
            updateReceiver,
            IntentFilter("com.example.qualcomm_teste_6.LOCATION_UPDATE")
        )

        startLocationService()

        // Força uma atualização inicial
        forceLocationUpdate()

        setContent {
            QUALCOMMTESTE6Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val ipPublic = utils.getIpPublic()

                    utils.getIPAddress(applicationContext)?.let {
                        LocationScreen(
                            lastLocationResult = lastLocationResult,
                            lastUpdateTime = lastUpdateTime,
                            isLoading = isLoading,
                            percentageBattery = batteryPercentage,
                            wifiSignalStrength = wifiSignalStrength,
                            mobileSignalStrength = mobileSignalStrength,
                            imei = imeiDevide,
                            model = modelDevice,
                            manufacturer = brandDevice,
                            ipAddress = utils.getIPAddress(applicationContext)!!,
                            idAddressPublic = ipPublic.toString(),
                            onForceUpdate = { forceLocationUpdate() },
                            onRefreshCache = { readLocationResult() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(updateReceiver)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao desregistrar receiver: ${e.message}")
        }
        XpsSingleton.abortXps()
    }

    private fun startLocationService() {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        )

        val hasAllPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasAllPermissions) {
            val serviceIntent = Intent(this, LocationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d("MainActivity", "Serviço de localização iniciado")
        } else {
            Log.e("MainActivity", "Permissões necessárias não concedidas.")
            requestPermissions() // Solicita as permissões novamente
        }
    }

    private fun forceLocationUpdate() {
        isLoading = true

        loadingJob = CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            isLoading = false
        }

        // Envia um comando para o serviço forçar uma atualização
        val serviceIntent = Intent(this, LocationForegroundService::class.java).apply {
            action = "FORCE_UPDATE"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Lê os resultados após um pequeno delay
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            readLocationResult()
        }
    }

    private fun readLocationResult() {
        val sharedPref = getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        lastLocationResult = sharedPref.getString("lastLocationResult", "Nenhum resultado ainda.") ?: "Nenhum resultado ainda."
        lastUpdateTime = sharedPref.getString("lastUpdateTime", "Nenhuma busca realizada.") ?: "Nenhuma busca realizada."

        val result = lastLocationResult

        try {
            if (result.contains("Bateria:")) {
                val batery = result.substringAfter("Bateria: ").substringBefore("%")
                batteryPercentage = batery
            }

            if (result.contains("Wi-fi:")) {
                val wifiSignal = result.substringAfter("Wi-fi: ").substringBefore("dBm")
                wifiSignalStrength = wifiSignal
            }

            if (result.contains("Móvel:")) {
                val mobileSignal = result.substringAfter("Móvel: ").substringBefore("dBm")
                mobileSignalStrength = mobileSignal
            }

            if (result.contains("IMEI:")) {
                val imei = result.substringAfter("IMEI: ").substringBefore(",")
                imeiDevide = imei
            }

            if (result.contains("Marca:")) {
                val mcarca = result.substringAfter("Marca: ").substringBefore(",")
                brandDevice = mcarca
            }

            if (result.contains("Modelo:")) {
                val modelo = result.substringAfter("Modelo: ").substringBefore(",")
                modelDevice = modelo
            }

            if (result.contains("Ip Location:")) {
                val ipLocation = result.substringAfter("Ip Location: ").substringBefore("")
                ipLocalidade = ipLocation
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao processar resultado: ${e.message}")
        }
    }
}

@Composable
fun LocationScreen(
    lastLocationResult: String,
    lastUpdateTime: String,
    isLoading: Boolean,
    percentageBattery: String,
    wifiSignalStrength: String,
    mobileSignalStrength: String,
    imei: String?,
    manufacturer: String,
    model: String,
    ipAddress: String,
    idAddressPublic: String,
    onForceUpdate: () -> Unit,
    onRefreshCache: () -> Unit
) {

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            onRefreshCache()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Allcomtelecom",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Text(
            text = "Hora atual: $currentTime",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Último resultado:\n$lastLocationResult",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Última busca: $lastUpdateTime",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Porcentagem Bateria: $percentageBattery%",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Sinal Wi-Fi: $wifiSignalStrength dBm",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Sinal Móvel: $mobileSignalStrength dBm",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "IMEI: $imei",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Marca: $manufacturer",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center

        )

        Text(
            text = "Modelo: $model",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center

        )

        Text(
            text = "IP: ${ipAddress ?: "Indisponível"}",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onForceUpdate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)

            ) {
                Text(text = "Forçar Atualização")
            }

            Spacer(modifier = Modifier.padding(16.dp))

            Button(
                onClick = onRefreshCache,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(text = "Atualizar Cache")
            }
        }
    }
}


