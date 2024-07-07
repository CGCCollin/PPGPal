package com.cgcworks.ppgpal.Receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cgcworks.ppgpal.Activities.MainActivity

open class AccelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.getStringExtra("data")
        data?.let {
            (context as MainActivity).updateAccelData(it)
        }
    }
}
