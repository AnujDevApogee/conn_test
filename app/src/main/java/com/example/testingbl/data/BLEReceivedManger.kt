package com.example.testingbl.data

interface BLEReceivedManger {

//    val data: MutableSharedFlow<Response<TemperatureData>>

    fun reconnect():Boolean

    fun disconnect()


    fun startReceivingData()

    fun closeConnection()


}