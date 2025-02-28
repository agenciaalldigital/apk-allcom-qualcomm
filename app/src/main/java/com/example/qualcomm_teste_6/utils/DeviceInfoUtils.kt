package com.example.qualcomm_teste_6.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

object DeviceInfoUtils {
    fun getBatteryPercentage(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getWifiSignalStrength(context: Context): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo.rssi
    }

    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER
    }

    fun getDeviceModel(): String {
        return Build.MODEL
    }

    fun getIpPublic(): String? {
        return try {
            URL("https://api.ipify.org").readText()
        } catch (e: Exception) {
            Log.e("DeviceInfoUtils", "Erro ao obter IP público: ${e.message}")
            null
        }
    }

    @SuppressLint("DefaultLocale")
    fun getIPAddress(context: Context): String? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return if (capabilities != null) {
            when {

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wifiManager =
                        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    val ipAddress = wifiInfo.ipAddress
                    String.format(
                        "%d.%d.%d.%d",
                        ipAddress and 0xff,
                        ipAddress shr 8 and 0xff,
                        ipAddress shr 16 and 0xff,
                        ipAddress shr 24 and 0xff
                    )
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    // Obter IP para dados móveis
                    try {
                        val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                        for (networkInterface in networkInterfaces) {
                            for (address in Collections.list(networkInterface.inetAddresses)) {
                                if (!address.isLoopbackAddress && address.hostAddress?.contains(":") == false) {
                                    return address.hostAddress
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    null
                }
                else -> null
            }
        } else {
            null
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String? {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                return "Permissão não concedida"
            }

            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    telephonyManager.imei
                }
                else -> {
                    telephonyManager.deviceId
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceInfo", "Erro ao obter ID do dispositivo: ${e.message}")
            return "Erro: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    fun getMobileSignalStrength(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1

        val telephonyManager = context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cellInfo = telephonyManager.allCellInfo
        var signalStrength = -1

        if (cellInfo != null) {
            for (info in cellInfo) {
                if (info.isRegistered) {
                    when (info) {
                        is CellInfoGsm -> signalStrength = info.cellSignalStrength.dbm
                        is CellInfoCdma -> signalStrength = info.cellSignalStrength.dbm
                        is CellInfoLte -> signalStrength = info.cellSignalStrength.dbm
                        is CellInfoWcdma -> signalStrength = info.cellSignalStrength.dbm
                        is CellInfoNr -> signalStrength = info.cellSignalStrength.dbm
                    }
                    break
                }
            }
        }

        return signalStrength
    }
}