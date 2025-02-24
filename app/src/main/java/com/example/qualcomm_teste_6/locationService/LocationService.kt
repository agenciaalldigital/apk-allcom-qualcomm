package com.example.qualcomm_teste_6.locationService

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.skyhookwireless.wps.IWPS
import com.skyhookwireless.wps.WPSLocationCallback
import com.skyhookwireless.wps.WPSLocation
import com.skyhookwireless.wps.WPSReturnCode
import com.skyhookwireless.wps.WPSStreetAddressLookup
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LocationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 15_000L // 15 segundos
    private lateinit var xps: IWPS

    override fun onCreate() {
        super.onCreate()
        // Configuração inicial do xps aqui, caso necessário
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.postDelayed(object : Runnable {
            override fun run() {
                getLocation()
                handler.postDelayed(this, interval) // Repetir após 15 segundos
            }
        }, interval)

        return START_STICKY
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getLocation() {
        // Aqui você pode chamar o seu método `determineLocation` para obter a localização
        GlobalScope.launch(Dispatchers.IO) {
            // Implemente a função que chama a localização aqui, como no seu código original
            // Este código pode ser adaptado com o xps.getLocation
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Limpar os callbacks
    }
}
