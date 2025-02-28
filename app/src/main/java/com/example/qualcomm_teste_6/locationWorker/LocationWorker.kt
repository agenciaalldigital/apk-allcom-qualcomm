package com.example.qualcomm_teste_6.locationWorker

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.skyhookwireless.wps.IWPS
import com.skyhookwireless.wps.WPSContinuation
import com.skyhookwireless.wps.WPSLocation
import com.skyhookwireless.wps.WPSLocationCallback
import com.skyhookwireless.wps.WPSReturnCode
import com.skyhookwireless.wps.WPSStreetAddressLookup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.qualcomm_teste_6.network.RetrofitClient
import com.example.qualcomm_teste_6.network.CreateRequestPostLocation
import com.example.qualcomm_teste_6.utils.DeviceInfoUtils
import com.example.qualcomm_teste_6.utils.PreferencesUtils
import com.example.qualcomm_teste_6.utils.XpsSingleton
import com.skyhookwireless.wps.WPSPeriodicLocationCallback

class LocationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    val xps = XpsSingleton.getXps()
    private val utils = DeviceInfoUtils
    private val utilsPrerence = PreferencesUtils

    override fun doWork(): Result {

        try {
            xps.setTunable("LogEnabled", true)
            xps.setKey("eJwNwcENACEIBMC3xZC4KiJfxG3qcr3rDArqA-iy8lEj1A-lOU0MJ2Vwh3hfiZGT8PZfGIILYA")
            xps.setTunable("ObservabilityEnabled", true)
            xps.setTunable("RemoteCacheMaxSize", true)

        } catch (e: IllegalArgumentException) {
            Log.e("LocationWorker", "Erro ao configurar a chave: ${e.message}")
            return Result.failure()
        }

        determineLocation(xps)

        return Result.success()
    }

    private fun determineLocation(xps: IWPS) {
        val batteryPercentage = utils.getBatteryPercentage(applicationContext)
        val wifiSignalStrength = utils.getWifiSignalStrength(applicationContext)
        val imei = utils.getDeviceId(applicationContext) // Obtém o IMEI
        val manufacturer = utils.getDeviceManufacturer() // Obtém a marca
        val model = utils.getDeviceModel()
        val mobileSignalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getMobileSignalStrength(applicationContext)
        } else {
            TODO("VERSION.SDK_INT < Q")
        }

        var ipPublic = utils.getIpPublic()

        Log.e("INICIO", "ANTES BREAKING POINT")
        xps.getPeriodicLocation(
            null,
            WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP, // streetAddressLookup
            false,
            10000L,
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
                    utilsPrerence.saveLocationResult(
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
                    utilsPrerence.saveLastUpdateTime(applicationContext)

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
                        ipPublicAddress =  ipPublic.toString()
                    )

                    // Retorna WPS_CONTINUE para continuar as atualizações
                    return WPSContinuation.WPS_CONTINUE
                }

                override fun done() {
                    // Chamado quando todas as iterações são concluídas
                    Log.d("LocationWorker", "Atualizações de localização concluídas.")
                }

                override fun handleError(returnCode: WPSReturnCode): WPSContinuation {
                    Log.e("LocationWorker", "Erro na localização: $returnCode")
                    return WPSContinuation.WPS_CONTINUE // Continua as atualizações
                }
            }
        )

        Log.e("FIM", "DEPOIS BREAKING POINT")

    }


    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMobileSignalStrength(context: Context): Int {
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


    private fun makePostRequest(lat: String, long: String, country: String, data: String, batery: Int, imei: String, brand: String, model: String, ipAddress: String, ipPublicAddress: String) {
        val apiService = RetrofitClient.instance
        val request = CreateRequestPostLocation(lat, long, country, data, batery, imei, brand, model, ipAddress, ipPublicAddress)

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
