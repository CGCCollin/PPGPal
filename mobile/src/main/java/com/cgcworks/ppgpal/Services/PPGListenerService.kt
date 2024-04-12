package com.cgcworks.ppgpal.Services

import android.content.Intent
import android.os.Environment
import android.util.Log
import kotlin.coroutines.*
import com.cgcworks.ppgpal.Activities.MainActivity
import com.cgcworks.ppgpal.datatstructs.Accelerometer
import com.cgcworks.ppgpal.datatstructs.PPGGreen
import com.cgcworks.ppgpal.datatstructs.PPGRed
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PPGListenerService: WearableListenerService() {


    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(Companion.TAG, "onMessageReceived(): $messageEvent")
        Log.d(Companion.TAG, String(messageEvent.data))
        if(messageEvent.path == PPG_RED_PATH){
            val ppgRed = String(messageEvent.data)
            GlobalScope.launch(Dispatchers.IO) {
                writeJson("PPGRed", jsonifyPPGRed(ppgRed), ppgRedWrites++)
            }

        }
        else if(messageEvent.path == PPG_GREEN_PATH){
            val ppgGreen = String(messageEvent.data)
            GlobalScope.launch(Dispatchers.IO) {
                writeJson("PPGGreen", jsonifyPPGGreen(ppgGreen), ppgGreenWrites++)
            }
        }
        else if (messageEvent.path == ACCEL_PATH){
            val accel = String(messageEvent.data)
            GlobalScope.launch(Dispatchers.IO) {
                writeJson("Accelerometer", jsonifyAccelerometer(accel), accelWrites++)
            }
        }
        else{
            Log.e(TAG, "Unknown message path: ${messageEvent.path}")
        }
    }
    fun jsonifyPPGGreen(input: String): String {
        val jsonArray = JSONArray()
        val objects = input.split(";")
        objects.forEach {
            try{
            val parts = it.split(",")
            val ppgGreen = PPGGreen(parts[0].toLong(), parts[1].toInt())
            jsonArray.put(JSONObject().put("timestamp", ppgGreen.getTimestamp()).put("green", ppgGreen.getPPG()))
                }catch (e: Exception){
                Log.e(TAG, "Failed to parse PPGGreen", e)
            }
        }
        return jsonArray.toString()
    }

    fun jsonifyPPGRed(input: String): String {
        val jsonArray = JSONArray()
        val objects = input.split(";")
        objects.forEach {
            try {
                val parts = it.split(",")
                val ppgRed = PPGRed(parts[0].toLong(), parts[1].toInt())
                jsonArray.put(
                    org.json.JSONObject().put("timestamp", ppgRed.getTimestamp())
                        .put("red", ppgRed.getRed())
                )
            }catch (e: Exception){
                Log.e(TAG, "Failed to parse PPGRed", e)
            }
        }
        return jsonArray.toString()
    }


    fun jsonifyAccelerometer(input: String): String {
        val jsonArray = JSONArray()
        val objects = input.split(";")
        objects.forEach {
            try {
                val parts = it.split(",")
                val accelerometer = Accelerometer(
                    parts[0].toLong(),
                    parts[1].toInt(),
                    parts[2].toInt(),
                    parts[3].toInt()
                )
                jsonArray.put(
                    org.json.JSONObject().put("timestamp", accelerometer.getTimestamp())
                        .put("x", accelerometer.getX())
                        .put("y", accelerometer.getY())
                        .put("z", accelerometer.getZ())
                )
            }catch (e: Exception){
                Log.e(TAG, "Failed to parse Accelerometer", e)
            }
        }
        return jsonArray.toString()
    }

    fun writeJson(name: String, json: String, writeNumber: Int){
        Log.d(TAG, "Writing JSON to file")
        Log.w(TAG, "writeNumber: $writeNumber, name: $name")
        try {
            if (ppgGreenWrites == 0 && ppgRedWrites == 0 && accelWrites == 0) {
                val dir = "${WRITE_EXTERNAL_STORAGE_PATH}/${MAIN_DATA_FOLDER}"
                val directory = File(dir)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
            }
            if (writeNumber == 0) {
                val dir = "${WRITE_EXTERNAL_STORAGE_PATH}/${MAIN_DATA_FOLDER}/${name}"
                val directory = File(dir)
                if(directory.exists()){
                    directory.deleteRecursively()
                }
                directory.mkdirs()
            }
            val file =
                File("${WRITE_EXTERNAL_STORAGE_PATH}/${MAIN_DATA_FOLDER}/${name}/${name}_${writeNumber}.json")
            file.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write JSON to file", e)
        }

    }

    companion object {
        private const val MESSAGE_PATH = "/PPG_DATA"
        private const val TAG = "PPGListenerService"
        private const val PPG_GREEN_PATH = "/PPG_GREEN"
        private const val PPG_RED_PATH = "/PPG_RED"
        private const val ACCEL_PATH = "/ACCELEROMETER"
        private var ppgGreenWrites = 0
        private var ppgRedWrites = 0
        private var accelWrites = 0
        private const val MAIN_DATA_FOLDER = "PPGPal"
        private val WRITE_EXTERNAL_STORAGE_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS)
    }

}
