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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.qualcomm_teste_6.locationWorker.LocationWorker
import com.example.qualcomm_teste_6.ui.theme.QUALCOMMTESTE6Theme
import com.example.qualcomm_teste_6.utils.DeviceInfoUtils
import com.skyhookwireless.wps.WPS
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.work.WorkInfo
import com.example.qualcomm_teste_6.utils.XpsSingleton
import kotlinx.coroutines.*

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
        val permissionsToRequest = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BATTERY_STATS
        ).filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest)
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

    private fun requestPhonePermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_PHONE_STATE
            ),
            PERMISSION_REQUEST_CODE
        )
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

        scheduleLocationWorker()

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
        unregisterReceiver(updateReceiver)
        XpsSingleton.abortXps()
    }

    private fun scheduleLocationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequest.Builder(
            LocationWorker::class.java,
            1,
            TimeUnit.HOURS
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LocationWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun forceLocationUpdate() {

        isLoading = true

        loadingJob = CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            isLoading = false
        }

        val workRequest = OneTimeWorkRequest.Builder(LocationWorker::class.java).build()
        WorkManager.getInstance(this).enqueue(workRequest)

        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(workRequest.id)
            .observe(this) { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        readLocationResult() // Atualiza os dados na tela
                    }
                    else -> {
                    }
                }
            }
    }

    private fun readLocationResult() {
        val sharedPref = getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        lastLocationResult = sharedPref.getString("lastLocationResult", "Nenhum resultado ainda.") ?: "Nenhum resultado ainda."
        lastUpdateTime = sharedPref.getString("lastUpdateTime", "Nenhuma busca realizada.") ?: "Nenhuma busca realizada."

        val result = lastLocationResult
        val batery = result.substringAfter("Bateria: ").substringBefore("%")
        val wifiSignal = result.substringAfter("Wi-Fi: ").substringBefore("dBm")
        val mobileSignal = result.substringAfter("Móvel: ").substringBefore("dBm")
        val imei = result.substringAfter("IMEI: ").substringBefore(",")
        val mcarca = result.substringAfter("Marca: ").substringBefore(",")
        val modelo = result.substringAfter("Modelo: ").substringBefore(",")
        val ipLocation = result.substringAfter("Ip Location: ").substringBefore("")

        Log.e("imei", imei)

        wifiSignalStrength = wifiSignal
        mobileSignalStrength = mobileSignal
        batteryPercentage = batery
        imeiDevide = imei
        brandDevice = mcarca
        modelDevice = modelo
        ipLocalidade = ipLocation
    }

    private fun getBatteryPercentage(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getWifiSignalStrength(context: Context): Int {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo.rssi
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