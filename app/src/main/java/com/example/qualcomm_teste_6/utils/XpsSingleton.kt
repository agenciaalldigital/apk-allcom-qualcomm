@file:Suppress("UNREACHABLE_CODE")

package com.example.qualcomm_teste_6.utils

import android.content.Context
import android.util.Log
import com.skyhookwireless.wps.XPS


object XpsSingleton {
    private lateinit var xps: XPS

    fun initialize(context: Context) {
        xps = XPS(context)
        xps.setKey("eJwNwcENACEIBMC3xZC4KiJfxG3qcr3rDArqA-iy8lEj1A-lOU0MJ2Vwh3hfiZGT8PZfGIILYA")

        XPS.setServerUrl("https://int-blue-aio-lb.skyhook.com/wps2")

        xps.setTunable("ObservabilityEnabled", true)
        xps.setTunable("LogEnabled", true);
        xps.setTunable("RemoteCacheMaxSize", 0)

    }

    fun getXps(): XPS {
        if(!::xps.isInitialized){
            throw  IllegalStateException("XPS não foi inicializado. Chame initialize() primeiro.");
            Log.e("XPSSINGLETON", "XPS não foi inicializado. Chame initialize() primeiro.");
        }

        return  xps
    }

    fun abortXps() {
        if (::xps.isInitialized) {
            xps.abort()
        }
    }
}