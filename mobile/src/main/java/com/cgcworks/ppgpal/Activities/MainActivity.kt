package com.cgcworks.ppgpal.Activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.cgcworks.ppgpal.databinding.ActivityMainBinding
import com.cgcworks.ppgpal.datatstructs.Accelerometer
import com.cgcworks.ppgpal.datatstructs.PPGGreen
import com.cgcworks.ppgpal.datatstructs.PPGRed
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ppgRedLabel: TextView
    private lateinit var ppgGreenLabel: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var perms: ArrayList<String>
    private val TAG = MainActivity::class.java.name
    private val PERMISSION_REQ_TAG = 1
    private lateinit var watchNode: Node


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Create a list of neccessary permissions
        perms = ArrayList<String>()
        perms.add(android.Manifest.permission.BLUETOOTH)
        perms.add(android.Manifest.permission.BLUETOOTH_ADMIN)
        perms.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        perms.add(android.Manifest.permission.ACCESS_FINE_LOCATION)

        binding = ActivityMainBinding.inflate(layoutInflater)

        //initialize view elements
        ppgRedLabel = binding.ppgRed
        ppgGreenLabel = binding.ppgGreen
        startButton = binding.startPpg
        stopButton = binding.stopPpg

        startButton.setOnClickListener(){
            Log.i(TAG, "Start Button Clicked")
            messaging("START")
        }

        stopButton.setOnClickListener() {
            Log.i(TAG, "Stop Button Clicked")
            messaging("STOP")
        }

        val view = binding.root
        setContentView(view)
        checkPermissions(this,perms.toTypedArray())
        getWriteExternalStoargePerms()
        setupHealthTrackingNodes()
    }

    fun requestPermission(){
        val it = perms.iterator()
        while(it.hasNext()){
            if((ActivityCompat.checkSelfPermission(this,it.next())
                == PackageManager.PERMISSION_GRANTED)
            ){
                it.remove()
            }
        }

        if(perms.isEmpty()){
            setResult(RESULT_OK)
            Log.i(TAG, "requestPermission: PERMISSIONS GRANTED.")
            finish()
        }
        else{
            var permissions: Array<String> = perms.toTypedArray()
            ActivityCompat.requestPermissions(this,permissions,this.PERMISSION_REQ_TAG)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "RequestPermissionResults")

        if(PERMISSION_REQ_TAG == 1){
            if (permissions.size == 0) {
                Log.i(
                    TAG,
                    "onRequestPermissionsResult : permission is 0"
                )
                return
            }

            for (p in grantResults){
                if(p == PackageManager.PERMISSION_DENIED){
                    Log.i(TAG, "onRequestPermissionResult: PERMISSION: " + p.toString() + "DENIED")
                }
            }
            this.recreate()
        }


    }

    fun checkPermissions(context: Context, permissions: Array<String>): Boolean{
        for (p in permissions){
            if(ActivityCompat.checkSelfPermission(context,p) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "checkPermissions: PERMISSION: " + p + "DENIED")
                    requestPermission()
                    return false

            }
        }


        Log.i(TAG, "checkPermissions: PERMISSIONS GRANTED")
        return true

    }

    private fun getWriteExternalStoargePerms() {
        if (Environment.isExternalStorageManager()) {
            Toast.makeText(
                this,
                "Storage Perms Granted. Will write results to JSON",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val uri = Uri.parse("package:${packageName}")
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                ), 200
            )
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(
                    this,
                    "Storage Perms Denied. Will not write results to JSON",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    fun messaging(message: String){
        watchNode.id?.also{nodeId ->
            val sendTask: Task<*> = Wearable.getMessageClient(this).sendMessage(
                nodeId,
                CONTROL_MESSAGE_PATH,
                message.toByteArray()
            ).apply {
                addOnSuccessListener {
                    Log.i(TAG, "Message sent successfully")
                }
                addOnFailureListener {
                    Log.e(TAG, "Failed to send message")
                }
            }
        }

    }


    private fun setupHealthTrackingNodes(){
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val capabilityInfo = Tasks.await(
                    Wearable.getCapabilityClient(applicationContext).getCapability(
                        HEALTH_TRACKING_CAPABILITY_NAME,
                        CapabilityClient.FILTER_REACHABLE
                    )
                )

                val connectedNodes = capabilityInfo.nodes
                val bestNode = connectedNodes.firstOrNull() // or use your own logic to select a node
                Log.i(TAG, "Connected nodes: $connectedNodes")
                bestNode?.let { node ->
                    // Use DataClient, MessageClient, or ChannelClient for communication
                    updatePPGCapability(node)
                } ?: Log.e(TAG, "No capable nodes found")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to get capability info", e)
            }
        }
    }

    private fun updatePPGCapability(node: Node){
        watchNode = node
    }



    companion object {
        private const val HEALTH_TRACKING_CAPABILITY_NAME = "health_transmit"
        private const val CONTROL_MESSAGE_PATH = "/CONTROL"
        private const val PPG_GREEN_PATH = "/PPG_GREEN"
        private const val PPG_RED_PATH = "/PPG_RED"
        private const val ACCEL_PATH = "/ACCELEROMETER"

    }

}