package com.example.qualcomm_teste_6.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.qualcomm_teste_6.MainActivity
import com.example.qualcomm_teste_6.R
import com.example.qualcomm_teste_6.utils.DeviceInfoUtils
import com.example.qualcomm_teste_6.utils.PreferencesUtils
import com.example.qualcomm_teste_6.utils.XpsSingleton
import com.skyhookwireless.wps.IWPS
import com.skyhookwireless.wps.WPSContinuation
import com.skyhookwireless.wps.WPSLocation
import com.skyhookwireless.wps.WPSPeriodicLocationCallback
import com.skyhookwireless.wps.WPSReturnCode
import com.skyhookwireless.wps.WPSStreetAddressLookup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.example.qualcomm_teste_6.network.CreateRequestPostLocation
import com.example.qualcomm_teste_6.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationForegroundService : Service() {
    private val CHANNEL_ID = "LocationServiceChannel"
    private val NOTIFICATION_ID = 1
    private val utils = DeviceInfoUtils
    private val utilsPreference = PreferencesUtils
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        XpsSingleton.initialize(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())
            startLocationUpdates()
        }

        // Se o serviço for encerrado pelo sistema, ele será reiniciado
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Serviço de Localização",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Serviço de Localização")
            .setContentText("Monitorando sua localização")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startLocationUpdates() {
        val xps = XpsSingleton.getXps()

        try {
            xps.setTunable("LogEnabled", true)
            xps.setKey("eJwNwcENACEIBMC3xZC4KiJfxG3qcr3rDArqA-iy8lEj1A-lOU0MJ2Vwh3hfiZGT8PZfGIILYA")
            xps.setTunable("ObservabilityEnabled", true)
            xps.setTunable("RemoteCacheMaxSize", true)
        } catch (e: IllegalArgumentException) {
            Log.e("LocationService", "Erro ao configurar a chave: ${e.message}")
        }

        determineLocation(xps)
    }

    private fun determineLocation(xps: IWPS) {
        val batteryPercentage = utils.getBatteryPercentage(applicationContext)
        val wifiSignalStrength = utils.getWifiSignalStrength(applicationContext)
        val imei = utils.getDeviceId(applicationContext)
        val manufacturer = utils.getDeviceManufacturer()
        val model = utils.getDeviceModel()
        val mobileSignalStrength = if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            utils.getMobileSignalStrength(applicationContext)
        } else {
            -1
        }

        var ipPublic = utils.getIpPublic()

        xps.getPeriodicLocation(
            null,
            WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP,
            false,
            1000L,
            0,
            object : WPSPeriodicLocationCallback {
                override fun handleWPSPeriodicLocation(location: WPSLocation): WPSContinuation {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val precision = location.hpe
                    val country = location.streetAddress.toString() ?: "Brasil"
                    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val ip = location.ip ?: "Sem Ip da location"

                    Log.d("DADOS", location.toString())

                    // Salva o resultado e a hora da última busca
                    utilsPreference.saveLocationResult(
                        applicationContext,
                        "Latitude: $latitude, " +
                                "Longitude: $longitude, " +
                                "Precisão: $precision, " +
                                "País: $country, " +
                                "Bateria: $batteryPercentage%" +
                                "Wi-fi: $wifiSignalStrength dBm" +
                                "Móvel: $mobileSignalStrength dBm" +
                                "IMEI: $imei," +
                                "Marca: $manufacturer," +
                                "Modelo: $model," +
                                "Ip Location: $ip"
                    )
                    utilsPreference.saveLastUpdateTime(applicationContext)

                    val intent = Intent("com.example.qualcomm_teste_6.LOCATION_UPDATE").apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        // Adicione dados extras se necessário
                        putExtra("updateTime", System.currentTimeMillis())
                    }
                    sendBroadcast(intent)
                    Log.d("LocationService", "Broadcast enviado!")

                    makePostRequest(
                        latitude.toString(),
                        longitude.toString(),
                        country,
                        timestamp,
                        batteryPercentage,
                        imei.toString(),
                        manufacturer,
                        model,
                        ipAddress = utils.getIPAddress(applicationContext)!!,
                        ipPublicAddress = ipPublic.toString()
                    )

                    return WPSContinuation.WPS_CONTINUE
                }

                override fun done() {
                    Log.d("LocationService", "Atualizações de localização concluídas.")
                }

                override fun handleError(returnCode: WPSReturnCode): WPSContinuation {
                    Log.e("LocationService", "Erro na localização: $returnCode")
                    return WPSContinuation.WPS_CONTINUE
                }
            }
        )
    }

    private fun makePostRequest(lat: String, long: String, country: String, data: String, batery: Int, imei: String, brand: String, model: String, ipAddress: String, ipPublicAddress: String) {
        val apiService = RetrofitClient.instance
        val request = CreateRequestPostLocation(lat, long, country, data, batery, imei, brand, model, ipAddress, ipPublicAddress)

        Thread {
            try {
                val response = apiService.createItem(request).execute()
                if (response.isSuccessful) {
                    Log.d("LocationService", "Item criado com sucesso! ID: ${response.body()?.id}")
                } else {
                    Log.e("LocationService", "Falha ao criar item: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("LocationService", "Erro na requisição: ${e.message}")
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}