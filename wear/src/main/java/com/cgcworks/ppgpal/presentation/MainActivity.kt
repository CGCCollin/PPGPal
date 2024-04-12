/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.cgcworks.ppgpal.presentation


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cgcworks.ppgpal.databinding.MainBinding
import com.cgcworks.ppgpal.backend.PPGRed
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTracker.TrackerEventListener
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainBinding
    private lateinit var ppg_red_label: TextView
    private lateinit var ppg_green_label: TextView
    private lateinit var healthTracking: HealthTrackingService
    private lateinit var connectionListener: ConnectionListener
    private var redTrackerListener: TrackerEventListener? = null
    private var greenTrackerListener: TrackerEventListener? = null
    private var accelTrackerListener: TrackerEventListener? = null
    private lateinit var ppgRedTracker: HealthTracker
    private lateinit var ppgGreenTracker: HealthTracker
    private lateinit var accelTracker: HealthTracker
    private lateinit var mPermissions: ArrayList<String>
    private val TAG: String = MainActivity::class.java.simpleName
    private val PERMISSION_KEY = "permissions"
    private val PERMISSION_REQ_TAG = 1
    private lateinit var info: PackageInfo;
    private var infer by Delegates.notNull<Boolean>(); //use this value to either make inferences, or disable them
    private val ppgRedData = ArrayList<PPGRed>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPermissions = ArrayList<String>()
        mPermissions.add(Manifest.permission.WAKE_LOCK)
        mPermissions.add(Manifest.permission.BODY_SENSORS)
        mPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        mPermissions.add(Manifest.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND)
        mPermissions.add(Manifest.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND)
        infer = true


        redTrackerListener = object: TrackerEventListener{
            override fun onDataReceived(data: List<DataPoint>) {
                //Log.d(TAG, "PPG_RED RECEIVED!")
                if(data.size > 0) {
                    val ppgRed = PPGRed(data.get(0).timestamp, data.get(0).getValue(ValueKey.PpgRedSet.PPG_RED))
                    ppgRedData.add(ppgRed)
                    if(ppgRedData.size > RED_BLOCK_SIZE){
                        val message = ppgRedData.joinToString(";") { it.stringify() }
                        messaging(message, PPG_RED_PATH)
                        ppgRedData.clear()
                    }
                }


                else{
                    Log.i(PPG_RED_TAG,"DATA RECEIVED: none")
                }


            }

            override fun onFlushCompleted() {
                Log.i(PPG_RED_TAG,"Tracker Listener: Flush complete!")
            }

            override fun onError(e: HealthTracker.TrackerError?) {
                Log.e(PPG_RED_TAG,"Tracker Error: ${e}")
            }



        }



        greenTrackerListener = object: TrackerEventListener{
            override fun onDataReceived(data: List<DataPoint>) {
                Log.d(TAG, "PPG_GREEN RECEIVED!")
                if(data.size > 0) {
                    var sum = 0
                    var tot = 0
                    Log.d(TAG, "LENGTH OF DATA LIST: ${data.size}")
                    var message = ""
                    for (i in 0 until data.size){
                        val point = data.get(i)
                        message+= "${point.timestamp},"
                        message+= "${point.getValue(ValueKey.PpgGreenSet.PPG_GREEN)}"
                        if(i < data.size - 1){
                            message += ";"
                        }

                    }
                    messaging(message, PPG_GREEN_PATH)
                }

                else{
                    Log.i(PPG_GREEN_TAG,"DATA RECEIVED: none")
                }

            }

            override fun onFlushCompleted() {
                Log.i(PPG_GREEN_TAG,"Tracker Listener: Flush complete!")
            }

            override fun onError(e: HealthTracker.TrackerError?) {
                Log.e(PPG_GREEN_TAG,"Tracker Error: ${e}")
            }

        }

        accelTrackerListener = object : TrackerEventListener{
            override fun onDataReceived(data: List<DataPoint>) {
                Log.d(TAG, "Received Accelerometer Data")
                if(data.size > 0) {
                    Log.d(TAG, "LENGTH OF DATA LIST: ${data.size}")
                    var message = ""
                    for (i in 0 until data.size){
                        val point = data.get(i)
                        message += "${point.timestamp},"
                        message += "${point.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_X)},"
                        message += "${point.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_Y)},"
                        message += "${point.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_Z)}"
                        if(i < data.size - 1){
                            message += ";"
                        }

                    }
                    messaging(message, ACCEL_PATH)
                }

                else{
                    Log.i(PPG_GREEN_TAG,"DATA RECEIVED: none")
                }

            }

            override fun onFlushCompleted() {
                Log.i(PPG_GREEN_TAG,"Tracker Listener: Flush complete!")
            }

            override fun onError(e: HealthTracker.TrackerError?) {
                Log.e(PPG_GREEN_TAG,"Tracker Error: ${e}")
            }

        }

        connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i("CONNECTION LISTENER" ,"Successful Connection! Starting PPG readings...")
                //Now let's grab a list of all available trackers
                val trackers = healthTracking.trackingCapability.supportHealthTrackerTypes
                for (tracker in trackers){
                    Log.i("AVAILABLE TRACKER","${tracker}")
                }

                //now let's try to grab a redPPG tracker
                try{
                    ppgRedTracker = healthTracking.getHealthTracker(HealthTrackerType.PPG_RED)
                    ppgGreenTracker = healthTracking.getHealthTracker(HealthTrackerType.PPG_GREEN)
                    accelTracker = healthTracking.getHealthTracker(HealthTrackerType.ACCELEROMETER)
                }catch(e: Exception){
                    Log.e(MainActivity::javaClass.name,"${e.printStackTrace()}")
                }
            }

            override fun onConnectionEnded() {
                Log.i("CONNECTION LISTENER" ,"Connection terminated. Flushing resources...")
            }

            override fun onConnectionFailed(e: HealthTrackerException?) {
                Log.e("CONNECTION LISTENER" ,"Connection Failed! Please try again!")
                if (e != null) {
                    if (e.hasResolution()){
                        e.resolve(this@MainActivity)
                    }
                }
            }

        }

        healthTracking = HealthTrackingService(connectionListener, this)
        healthTracking.connectService()
        binding = MainBinding.inflate(layoutInflater)
        val view = binding.root
        //Textviews
        ppg_red_label = binding.ppgRedLabel
        ppg_green_label = binding.ppgGreenLabel

        setContentView(view)
        connectToNode()
    }

    fun requestPermission() {
        val it = mPermissions!!.iterator()
        while (it.hasNext()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    it.next()
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                it.remove()
            }
        }
        if (mPermissions.isEmpty()) { // all allowed
            setResult(RESULT_OK)
            Log.i(TAG, "finished")
            finish()
        } else {
            var permissions: Array<String?>? = arrayOfNulls(
                mPermissions.size
            )
            permissions = mPermissions.toArray(permissions)
            ActivityCompat.requestPermissions(
                this,
                permissions,
                this.PERMISSION_REQ_TAG
            )
            Log.i(
                TAG,
                "requestPermissions"
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(
            TAG,
            "onRequestPermissionsResult() requestCode = " + "requestCode"
        )
        if (PERMISSION_REQ_TAG == 1) {
            if (permissions.size == 0) {
                Log.i(
                    TAG,
                    "onRequestPermissionsResult : permission is 0"
                )
                return
            }
            for (p in grantResults) {
                if (p == PackageManager.PERMISSION_DENIED) {
                    Log.i(
                        TAG,
                        "onRequestPermissionsResult : permission denied"
                    )
                    finish()
                    return
                }
            }
            setResult(RESULT_OK)
            Log.d("END OF ON REQ RESULT" ,"TEST")
            this.recreate()
        }
    }

    //run before starting the ppg reading
    //if this returns false, we should be requesting permissions.
    fun checkPermission(context: Context?, permissions: ArrayList<String>): Boolean {
        for (permission in permissions) {
            if (context == null || ActivityCompat.checkSelfPermission(
                    context,
                    permission!!
                ) == PackageManager.PERMISSION_DENIED
            ) {
                Log.i(
                    TAG,
                    "checkPermission : PERMISSION_DENIED : " + "${permission}"
                )
                return false
            } else {
                Log.i(
                    TAG,
                    "checkPermission : PERMISSION_GRANTED : " + "${permission}"
                )
            }
        }
        return true
    }

    private fun getNodes(): Collection<String> {
        return Tasks.await(Wearable.getNodeClient(this).connectedNodes).map { it.id }
    }


    override fun onDestroy() {
        super.onDestroy()
        //.Disconnect the tracking service.
        stopTracking(ppgRedTracker)
        stopTracking(ppgGreenTracker)
        stopTracking(accelTracker)
        healthTracking.disconnectService()
    }


    private fun startTracking(tracker: HealthTracker, trackerEventListener: TrackerEventListener){
        Log.d(TAG,"Start tracking: " + tracker::javaClass.name)
        if(checkPermission(this, mPermissions)) {
            try {
                tracker.setEventListener(trackerEventListener)
            } catch (e: Exception) {
                Log.e(TAG, "COULD NOT START: " + tracker::javaClass.name)
            }
        }
        else{
            requestPermission()
            Toast.makeText(this,"Need body sensor permission to perform basic function",Toast.LENGTH_SHORT)
        }
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("controlMessage")
            if (message != null) {
                healthTrackerControl(message)
            }

        }
    }

    //unsets the event listener for the red tracker to stop receiving data
    private fun stopTracking(tracker: HealthTracker){
        Log.d(TAG,"Stop tracking: " + tracker::javaClass.name)
        try {
            tracker.unsetEventListener()
        }catch(e: Exception){
            Log.e(TAG,"COULD NOT STOP: " + tracker::javaClass.name)
        }

    }

    //when watch display turns off, data is batched. can flush to receive it whenever display is turned on
    private fun flush_ppg_red(){
        ppgRedTracker.flush()
    }

    private fun messaging(message: String, path: String){
        lifecycleScope.launch(Dispatchers.IO){
            targetNode?.also{nodeId ->
                val sendTask: Task<*> = Wearable.getMessageClient(this@MainActivity).sendMessage(
                    nodeId,
                    path,
                    message.toByteArray()
                ).apply {
                    addOnSuccessListener {
                        Log.i(TAG, "Message sent successfully, path: ${path}")
                    }
                    addOnFailureListener {
                        Log.e(TAG, "Failed to send message")
                    }
                }
            }
        }
    }

    private fun connectToNode(){
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connectedNodes = getNodes()
                val bestNode = connectedNodes.firstOrNull()
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

    private fun updatePPGCapability(node: String){
        targetNode = node
    }

    fun healthTrackerControl(message: String){
        Log.i(TAG, "Received Control Code: " + message)
        if(message == START_CMD){
            if(greenTrackerListener == null || redTrackerListener == null){
                Log.e(TAG, "Tracker Listeners are null")
                return
            }
            startTracking(ppgGreenTracker, greenTrackerListener!!)
            startTracking(ppgRedTracker, redTrackerListener!!)
            startTracking(accelTracker, accelTrackerListener!!)

        }
        else if (message == STOP_CMD){
            stopTracking(ppgGreenTracker)
            stopTracking(ppgRedTracker)
            stopTracking(accelTracker)

        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            messageReceiver, IntentFilter("com.example.ACTION_MESSAGE_RECEIVED")
        )
        isForeground = true
    }

    override fun onStop() {
        super.onStop()
        isForeground = false
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }




    companion object{
        private const val PPG_GREEN_PATH = "/PPG_GREEN"
        private const val PPG_RED_PATH = "/PPG_RED"
        private const val ACCEL_PATH = "/ACCELEROMETER"
        private const val HEALTH_TRACKING_CAPABILITY_NAME = "ppg_receive"
        private const val CONTROL_MESSAGE_PATH = "/CONTROL"
        private const val PPG_GREEN_TAG = "PPG_GREEN"
        private const val PPG_RED_TAG = "PPG_RED"
        private const val TAG = "PPG_PAL WEAR"
        private const val START_CMD = "START"
        private const val STOP_CMD = "STOP"
        private const val RED_BLOCK_SIZE = 299
        @JvmField
        var isForeground = false
        private var targetNode: String? = null
    }

}





