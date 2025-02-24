package com.example.qualcomm_teste_6

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import com.example.qualcomm_teste_6.ui.theme.QUALCOMMTESTE6Theme
import com.skyhookwireless.wps.IWPS
import com.skyhookwireless.wps.WPS
import com.skyhookwireless.wps.XPS

class MainActivity : ComponentActivity() {
    private lateinit var xps: IWPS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WPS.setServerUrl("https://int-blue-aio-lb.skyhook.com/wps2")

        xps = XPS(this)
        try {
            xps.setKey("eJwNwcENACEIBMC3xZC4KiJfxG3qcr3rDArqA-iy8lEj1A-lOU0MJ2Vwh3hfiZGT8PZfGIILYA")
        } catch (e: IllegalArgumentException) {
            // Handle key error
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0
        )

        setContent {
            QUALCOMMTESTE6Theme {
                MainScreen(xps)
            }
        }
    }
}