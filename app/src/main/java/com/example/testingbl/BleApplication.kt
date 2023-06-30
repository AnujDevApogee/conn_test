package com.example.testingbl

import android.app.Application
import com.example.testingbl.di.BluetoothCommunication

class BleApplication :Application() {
    override fun onCreate() {
        super.onCreate()
        BluetoothCommunication.getInstance(applicationContext)
    }
}