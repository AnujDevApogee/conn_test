package com.example.testingbl.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.testingbl.data.ConnectionState
import com.example.testingbl.data.TemperatureAndHumidityReceivedManger
import com.example.testingbl.data.TemperatureData
import com.example.testingbl.utils.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class TemperatureAndHumidityBLEReceivedManger(
    private val bluetoothAdapter: BluetoothAdapter, private val context: Context
) : TemperatureAndHumidityReceivedManger {


    private val DEVICE_NAME = "Mivi Collar D25"
    private val TEMP_HUMIDITY_SERVICE_UIID =
        "0000aa20-0000-1000-8000-00805f9b34fb" // Need the Service UIID
    private val TEMP_HUMIDITY_CHARACTERISTICS_UUID =
        "0000aa21-0000-1000-8000-00805f9b34fb" // Need CHARACTERISTICS uuid

    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    override val data: MutableSharedFlow<Response<TemperatureData>>
        get() = MutableSharedFlow()


    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSetting = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)


    private var gatt: BluetoothGatt? = null


    private var isScanning = false


    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result?.device?.name == DEVICE_NAME) {
                coroutineScope.launch {
                    data.emit(Response.Loading(message = "Connecting with ${result.device.name}"))
                }
                if (isScanning) {
                    result.device.connectGatt(
                        context, false, gattCallback, BluetoothDevice.TRANSPORT_LE
                    )
                    //BluetoothDevice.TRANSPORT_LE only if when you use normal device
                    isScanning = false
                    bleScanner.stopScan(this)
                }
            }
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    coroutineScope.launch {
                        data.emit(Response.Loading(message = "Discovering Services..."))
                    }
                    gatt.discoverServices()
                    this@TemperatureAndHumidityBLEReceivedManger.gatt = gatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
                        data.emit(
                            Response.Success(
                                data = TemperatureData(
                                    0f, 0f, ConnectionState.DisConnected
                                )
                            )
                        )
                    }
                    gatt.close()
                } else {
                    gatt.close()
                    currentConnectionAttempt += 1
                    coroutineScope.launch {
                        data.emit(
                            Response.Loading(message = "Attempting to connect $currentConnectionAttempt / $MAXIMUM_CONNECTION_ATTEMPTS")
                        )
                    }

                    if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                        startReceivingData()
                    } else {
                        coroutineScope.launch {
                            data.emit(Response.Error("Cannot Connect to device"))
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                printGattTable()
                coroutineScope.launch {
                    data.emit(Response.Loading(message = "Adjusting MTU Space.."))
                }
                gatt.requestMtu(517)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("MTU_RANGE", "onMtuChanged: Mtu range => $mtu")
            val characteristic =
                findCharacteristic(TEMP_HUMIDITY_SERVICE_UIID, TEMP_HUMIDITY_CHARACTERISTICS_UUID)
            if (characteristic == null) {
                coroutineScope.launch {
                    data.emit(Response.Error("Could not find temp and Humidity Publisher"))
                }
                return
            }
            enableNotification(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic){
                when(uuid){
                    UUID.fromString(TEMP_HUMIDITY_CHARACTERISTICS_UUID) -> {
                        //XX XX XX XX XX XX
                        val multiplicator = if(value.first().toInt()> 0) -1 else 1
                        val temperature = value[1].toInt() + value[2].toInt() / 10f
                        val humidity = value[4].toInt() + value[5].toInt() / 10f
                        val tempHumidityResult = TemperatureData(
                            multiplicator * temperature,
                            humidity,
                            ConnectionState.Connected
                        )
                        coroutineScope.launch {
                            data.emit(
                                Response.Success(data = tempHumidityResult)
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }

    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        val ccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }
        characteristic.getDescriptor(ccdUuid)?.let { cccdDescriptor->
            if(gatt?.setCharacteristicNotification(characteristic, true) == false){
                Log.d("BLEReceiveManager","set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        gatt?.let {
            descriptor.value=payload
            it.writeDescriptor(descriptor)
        }?: error("Not connected to a BLE device!")
    }

    private fun findCharacteristic(
        serviceUIID: String,
        characteristicUIID: String
    ): BluetoothGattCharacteristic? {
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUIID
        }?.characteristics?.find { characteristic ->
            characteristic.uuid.toString() == characteristicUIID
        }
    }



    override fun reconnect() {
       gatt?.connect()
    }

    override fun disconnect() {
       gatt?.disconnect()
    }
    //Manifest.permission.BLUETOOTH_SCAN
    override fun startReceivingData() {
        coroutineScope.launch {
            data.emit(Response.Loading(message = "Scanning Ble devices..."))
        }
        isScanning = true

        bleScanner.startScan(null, scanSetting.build(), scanCallback)
    }

    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic = findCharacteristic(TEMP_HUMIDITY_SERVICE_UIID, TEMP_HUMIDITY_CHARACTERISTICS_UUID)
        if(characteristic != null){
            disconnectCharacteristic(characteristic)
        }
        gatt?.close()
    }

    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic,false) == false){
                Log.d("TempHumidReceiveManager","set charateristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }


}