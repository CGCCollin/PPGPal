package com.cgcworks.ppgpal.services

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cgcworks.ppgpal.presentation.MainActivity
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class MessageHandler : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        val messageData = String(messageEvent.data,Charsets.UTF_8)
        Log.d(TAG, "Message Received: ${messageEvent.path}")
        Log.d(TAG, "Message Received: ${messageData}")
        if(!MainActivity.isForeground){
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra("controlMessage", messageData)
            startActivity(intent)
        }else{
            val intent = Intent("com.example.ACTION_MESSAGE_RECEIVED")
            intent.putExtra("controlMessage", messageData)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

    }
     companion object {
         private const val TAG = "MessageHandler"
     }
}