package com.example.testingbl.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BlueToothBroadCastReceiver(
    private val context: Context,
    onSystemCall: (intent: Intent) -> Unit
) {
    private val register = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                onSystemCall.invoke(p1)
            }
        }
    }

    fun registerReceiver(systemAction: String) {
        val intentFilter = IntentFilter(systemAction)
        context.registerReceiver(register, intentFilter)
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(register)
    }

}