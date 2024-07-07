package com.cgcworks.ppgpal.services

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cgcworks.ppgpal.presentation.MainActivity
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class MessageHandler : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        val messageData = String(messageEvent.data, Charsets.UTF_8)
        Log.d(TAG, "Message Received: ${messageEvent.path}")
        Log.d(TAG, "Message Received: $messageData")

        val isAppInBackground = isAppInBackground()

        if (isAppInBackground) {
            // Acquire wake lock to bring the app to the foreground
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyApp::MessageHandlerWakelock"
            )
            wakeLock.acquire(3000) // Acquire the wake lock for 3 seconds

            // Bring the activity to the foreground and pass the control message
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("controlMessage", messageData)
            startActivity(intent)
        } else {
            // Send the broadcast if the app is already in the foreground
            val intent = Intent("com.example.ACTION_MESSAGE_RECEIVED")
            intent.putExtra("controlMessage", messageData)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    // Function to check if the app is in the background
    private fun isAppInBackground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return true

        for (processInfo in runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (activeProcess in processInfo.pkgList) {
                    if (activeProcess == packageName) {
                        return false
                    }
                }
            }
        }
        return true
    }

    companion object {
        private const val TAG = "MessageHandler"
    }
}
