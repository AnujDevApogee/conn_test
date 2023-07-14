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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID

@SuppressLint("MissingPermission")
class TemperatureAndHumidityBLEReceivedManger(
    private val bluetoothAdapter: BluetoothAdapter, private val context: Context
) : TemperatureAndHumidityReceivedManger {


    private val DEVICE_NAME =
        "D9:2D:9A:10:91:98"//"NAVIK50-1.0__2328532"//"Mivi Collar D25"//"NAVIK200-1.1_15"
    private val TEMP_HUMIDITY_SERVICE_UIID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"

    //"00001800-0000-1000-8000-00805f9b34fb"// "0000aa20-0000-1000-8000-00805f9b34fb" // Need the Service UIID
    private val TEMP_HUMIDITY_CHARACTERISTICS_UUID =
        "6e400003-b5a3-f393-e0a9-e50e24dcca9e"//"00002a00-0000-1000-8000-00805f9b34fb"
    // "0000aa21-0000-1000-8000-00805f9b34fb" // Need CHARACTERISTICS uuid

    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    private val _data = MutableStateFlow<Response<TemperatureData>?>(null)
    val data: StateFlow<Response<TemperatureData>?>
        get() = _data


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
            if (result?.device?.name != null) {
                Log.i("deviceNm", "onScanResult: ${result.device.name}  ${result.device.address}")
            }
            if (result?.device?.address == DEVICE_NAME) {
                coroutineScope.launch {
                    _data.value =
                        (Response.Loading(message = "Connecting with ${result.device.name}"))
                }
                if (isScanning) {
                    result.device.connectGatt(
                        context, false, gattCallback, BluetoothDevice.TRANSPORT_LE
                    )/*, BluetoothDevice.TRANSPORT_LE
                    )*/
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
                        _data.value = (Response.Loading(message = "Discovering Services..."))
                    }
                    gatt.discoverServices()
                    this@TemperatureAndHumidityBLEReceivedManger.gatt = gatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
                        _data.value = (
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
                        _data.value = (
                                Response.Loading(message = "Attempting to connect $currentConnectionAttempt / $MAXIMUM_CONNECTION_ATTEMPTS")
                                )
                    }

                    if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                        startReceivingData()
                    } else {
                        coroutineScope.launch {
                            _data.value = (Response.Error("Cannot Connect to device"))
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                printGattTable()
                coroutineScope.launch {
                    _data.value = (Response.Loading(message = "Adjusting MTU Space.."))
                }
                gatt.requestMtu(512)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("MTU_RANGE", "onMtuChanged: Mtu range => $mtu")
            val characteristic =
                findCharacteristic(TEMP_HUMIDITY_SERVICE_UIID, TEMP_HUMIDITY_CHARACTERISTICS_UUID)
            if (characteristic == null) {
                coroutineScope.launch {
                    _data.value =
                        (Response.Error("Could not find temperature and Humidity Publisher"))
                }
                return
            }
            Log.d("MTU_RANGE", "onMtuChanged: MTU_DONE")
            enableNotification(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                when (uuid) {
                    UUID.fromString(TEMP_HUMIDITY_CHARACTERISTICS_UUID) -> {
                        //XX XX XX XX XX XX
                        val bytes = String(value)
                        Log.i("INFO_BLE", "onCharacteristicChanged: $bytes")
                        val multiplicator = 22//if (value.first().toInt() > 0) -1 else 1
                        val temperature = 11//value[1].toInt() + value[2].toInt() / 10f
                        val humidity = 24//value[4].toInt() + value[5].toInt() / 10f
                        val tempHumidityResult = TemperatureData(
                            multiplicator * temperature.toFloat(),
                            humidity.toFloat(),
                            ConnectionState.Connected
                        )
                        coroutineScope.launch {
                            _data.value = (
                                    Response.Success(data = tempHumidityResult)
                                    )
                        }
                    }

                    else -> Unit
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.i("INFO_RES", "onCharacteristicWrite: ${characteristic?.service}")
        }

    }

    fun writeSample() {
        val char =
            findCharacteristic(TEMP_HUMIDITY_SERVICE_UIID, TEMP_HUMIDITY_CHARACTERISTICS_UUID)
        val ccdUuid = UUID.fromString(TEMP_HUMIDITY_CHARACTERISTICS_UUID)
        char?.service?.getCharacteristic(ccdUuid)?.let { charis ->
            val string = "unlog psrdopa"+"\r\n"
            gatt?.let {
                charis.value = string.toByteArray(StandardCharsets.UTF_8)
                val op = it.writeCharacteristic(charis)
                Log.d("MTU_RANGE", "writeDescription: DONE TESTING BLUETOOTH Send $op and $string")
            }
        }
    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        val ccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.i("MTU_RANGE", "enableNotification: else is here")
                return
            }
        }
        characteristic.getDescriptor(ccdUuid)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.d("BLEReceiveManager", "set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        gatt?.let {
            descriptor.value = payload
            val op = it.writeDescriptor(descriptor)
            Log.d("MTU_RANGE", "writeDescription: DONE TESTING BLUETOOTH Send $op")
        } ?: error("Not connected to a BLE device!")
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
            _data.value = (Response.Loading(message = "Scanning Ble devices..."))
        }
        isScanning = true

        bleScanner.startScan(null, scanSetting.build(), scanCallback)
    }

    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic =
            findCharacteristic(TEMP_HUMIDITY_SERVICE_UIID, TEMP_HUMIDITY_CHARACTERISTICS_UUID)
        if (characteristic != null) {
            disconnectCharacteristic(characteristic)
        }
        gatt?.close()
    }

    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.d("TempHumidReceiveManager", "set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }


}