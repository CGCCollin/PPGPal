package com.cgcworks.ppgpal.Receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cgcworks.ppgpal.Activities.MainActivity

open class PPGRedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.getStringExtra("data")
        data?.let {
            (context as MainActivity).updatePPGRedData(it)
        }
    }
}
