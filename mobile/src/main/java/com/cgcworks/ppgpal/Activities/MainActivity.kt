package com.cgcworks.ppgpal.Activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.cgcworks.ppgpal.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ppgRedLabel: TextView
    private lateinit var ppgGreenLabel: TextView
    private lateinit var accelerometerLabel: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var perms: ArrayList<String>
    private val TAG = MainActivity::class.java.name
    private val PERMISSION_REQ_TAG = 1
    private lateinit var watchNode: Node
    private var lastMessageSentTime: Long = 0

    private lateinit var ppgRedReceiver: BroadcastReceiver
    private lateinit var ppgGreenReceiver: BroadcastReceiver
    private lateinit var accelReceiver: BroadcastReceiver

    private var ppgRedValues = mutableListOf<Int>()
    private var ppgGreenValues = mutableListOf<Int>()
    private var accelValues = mutableListOf<Triple<Int, Int, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity created")

        // Create a list of necessary permissions
        perms = ArrayList()
        perms.add(android.Manifest.permission.BLUETOOTH)
        perms.add(android.Manifest.permission.BLUETOOTH_ADMIN)
        perms.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        perms.add(android.Manifest.permission.ACCESS_FINE_LOCATION)

        binding = ActivityMainBinding.inflate(layoutInflater)

        // Initialize view elements
        ppgRedLabel = binding.ppgRed
        ppgGreenLabel = binding.ppgGreen
        accelerometerLabel = binding.accelerometer

        startButton = binding.startPpg
        stopButton = binding.stopPpg

        startButton.setOnClickListener {
            Log.i(TAG, "Start Button Clicked")
            messaging(START_CMD)
        }

        stopButton.setOnClickListener {
            Log.i(TAG, "Stop Button Clicked")
            messaging(STOP_CMD)
        }

        val view = binding.root
        setContentView(view)
        checkPermissions(this, perms.toTypedArray())
        getWriteExternalStoragePerms()
        setupHealthTrackingNodes()

        ppgRedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val data = intent.getStringExtra("data")
                Log.d(TAG, "PPG Red Receiver received data: $data")
                data?.let {
                    updatePPGRedData(it)
                }
            }
        }

        ppgGreenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val data = intent.getStringExtra("data")
                Log.d(TAG, "PPG Green Receiver received data: $data")
                data?.let {
                    updatePPGGreenData(it)
                }
            }
        }

        accelReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val data = intent.getStringExtra("data")
                Log.d(TAG, "Accelerometer Receiver received data: $data")
                data?.let {
                    updateAccelData(it)
                }
            }
        }

        // Register receivers for new broadcast actions
    registerReceiver(ppgRedReceiver, IntentFilter(PPG_RED_BROADCAST), Context.RECEIVER_EXPORTED)
        registerReceiver(ppgGreenReceiver, IntentFilter(PPG_GREEN_BROADCAST), Context.RECEIVER_EXPORTED)
        registerReceiver(accelReceiver, IntentFilter(ACCEL_BROADCAST), Context.RECEIVER_EXPORTED)
    }

    fun requestPermission() {
        val it = perms.iterator()
        while (it.hasNext()) {
            if ((ActivityCompat.checkSelfPermission(this, it.next()) == PackageManager.PERMISSION_GRANTED)) {
                it.remove()
            }
        }

        if (perms.isEmpty()) {
            setResult(RESULT_OK)
            Log.i(TAG, "requestPermission: PERMISSIONS GRANTED.")
            finish()
        } else {
            val permissions: Array<String> = perms.toTypedArray()
            ActivityCompat.requestPermissions(this, permissions, this.PERMISSION_REQ_TAG)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "RequestPermissionResults")

        if (PERMISSION_REQ_TAG == 1) {
            if (permissions.isEmpty()) {
                Log.i(TAG, "onRequestPermissionsResult : permission is 0")
                return
            }

            for (p in grantResults) {
                if (p == PackageManager.PERMISSION_DENIED) {
                    Log.i(TAG, "onRequestPermissionResult: PERMISSION: $p DENIED")
                }
            }
            this.recreate()
        }
    }

    fun checkPermissions(context: Context, permissions: Array<String>): Boolean {
        for (p in permissions) {
            if (ActivityCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "checkPermissions: PERMISSION: $p DENIED")
                requestPermission()
                return false
            }
        }

        Log.i(TAG, "checkPermissions: PERMISSIONS GRANTED")
        return true
    }

    private fun getWriteExternalStoragePerms() {
        if (Environment.isExternalStorageManager()) {
            Toast.makeText(
                this,
                "Storage Perms Granted. Will write results to JSON",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val uri = Uri.parse("package:$packageName")
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

    fun messaging(message: String) {
        if (!message.equals(START_CMD) && !message.equals(STOP_CMD)){
            runOnUiThread{
                Toast.makeText(this,"Invalid Message. Aborting Transmission.", Toast.LENGTH_SHORT)
            }
            return
        }
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastMessageSentTime >= MESSAGE_INTERVAL_MS) {
            watchNode.id?.also { nodeId ->
                val sendTask: Task<*> = Wearable.getMessageClient(this).sendMessage(
                    nodeId,
                    CONTROL_MESSAGE_PATH,
                    message.toByteArray()
                ).apply {
                    addOnSuccessListener {
                        if (message.equals(START_CMD)) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Sent Start Signal",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        else if (message.equals(STOP_CMD)){
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Sent Stop Signal",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        Log.i(TAG, "Message sent successfully")
                    }
                    addOnFailureListener {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to send message.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.e(TAG, "Failed to send message")

                    }
                }
                lastMessageSentTime = currentTime
            }
        } else {
            Log.i(TAG, "Message not sent to avoid spamming")
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Please wait 2 seconds between control attemtps.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun setupHealthTrackingNodes() {
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

    private fun updatePPGCapability(node: Node) {
        watchNode = node
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(ppgRedReceiver)
        unregisterReceiver(ppgGreenReceiver)
        unregisterReceiver(accelReceiver)
    }

    fun updatePPGRedData(data: String) {
        val values = data.split(";").map { it.split(",")[1].toInt() }
        ppgRedValues.addAll(values)
        val average = ppgRedValues.average()
        ppgRedLabel.text = "PPG Red Avg: %.2f".format(average)
    }

    fun updatePPGGreenData(data: String) {
        val values = data.split(";").map { it.split(",")[1].toInt() }
        ppgGreenValues.addAll(values)
        val average = ppgGreenValues.average()
        ppgGreenLabel.text = "PPG Green Avg: %.2f".format(average)
    }

    fun updateAccelData(data: String) {
        val values = data.split(";").map {
            val parts = it.split(",")
            Triple(parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
        }
        accelValues.addAll(values)
        val avgX = accelValues.map { it.first }.average()
        val avgY = accelValues.map { it.second }.average()
        val avgZ = accelValues.map { it.third }.average()
        accelerometerLabel.text = "Accelerometer: x: %.2f, y: %.2f, z: %.2f".format(avgX, avgY, avgZ)
    }

    companion object {
        private const val HEALTH_TRACKING_CAPABILITY_NAME = "health_transmit"
        private const val CONTROL_MESSAGE_PATH = "/CONTROL"
        private const val PPG_GREEN_PATH = "/PPG_GREEN"
        private const val PPG_RED_PATH = "/PPG_RED"
        private const val ACCEL_PATH = "/ACCELEROMETER"
        private const val START_CMD = "START"
        private const val STOP_CMD = "STOP"
        const val PPG_RED_BROADCAST = "com.cgcworks.ppgpal.PPG_RED_BROADCAST"
        const val PPG_GREEN_BROADCAST = "com.cgcworks.ppgpal.PPG_GREEN_BROADCAST"
        const val ACCEL_BROADCAST = "com.cgcworks.ppgpal.ACCEL_BROADCAST"
        private const val MESSAGE_INTERVAL_MS = 2000
    }
}
