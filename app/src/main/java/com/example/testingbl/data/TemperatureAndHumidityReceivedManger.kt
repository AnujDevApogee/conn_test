package com.example.testingbl.data

import com.example.testingbl.utils.Response
import kotlinx.coroutines.flow.MutableSharedFlow

interface TemperatureAndHumidityReceivedManger {

//    val data: MutableSharedFlow<Response<TemperatureData>>

    fun reconnect()

    fun disconnect()


    fun startReceivingData()

    fun closeConnection()


}