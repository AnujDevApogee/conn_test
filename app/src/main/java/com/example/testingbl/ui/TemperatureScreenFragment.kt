package com.example.testingbl.ui

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.testingbl.R
import com.example.testingbl.data.ConnectionState
import com.example.testingbl.data.ble.TemperatureAndHumidityBLEReceivedManger
import com.example.testingbl.databinding.TemperatureLayoutBinding
import com.example.testingbl.di.BluetoothCommunication
import com.example.testingbl.utils.BlueToothBroadCastReceiver
import com.example.testingbl.utils.Response
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class TemperatureScreenFragment(private val enableBlue: () -> Unit) :
    Fragment(R.layout.temperature_layout) {

    private lateinit var binding: TemperatureLayoutBinding

    private var repo: TemperatureAndHumidityBLEReceivedManger? = null

    private var bluetoothReg: BlueToothBroadCastReceiver? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = TemperatureLayoutBinding.bind(view)
        bluetoothReg = BlueToothBroadCastReceiver(requireActivity()) {
            if (it.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                enableBlue.invoke()
            }
        }

        repo =
            TemperatureAndHumidityBLEReceivedManger(
                BluetoothCommunication.getInstance(requireActivity()).getBluetoothAdaptor(),
                requireActivity()
            )

        gettingData()


        binding.connectBtn.setOnClickListener {
            repo?.startReceivingData()
        }


        binding.writeBtn.setOnClickListener {
            repo?.writeSample()
        }

        binding.disConnectBtn.setOnClickListener {
            repo?.disconnect()
        }

        binding.reconnectBtn.setOnClickListener {
            Toast.makeText(activity, "RECONNECTED ${repo?.reconnect()}", Toast.LENGTH_SHORT).show()
        }


        binding.closeConnectBtn.setOnClickListener {
            repo?.closeConnection()
        }


    }

    private fun gettingData() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo?.data?.collect { res ->
                    res?.let {
                        when (it) {
                            is Response.Error -> {
                                Log.i("Type_RESPONSE", "Error onViewCreated: ${it.errorMessage}")
                            }

                            is Response.Loading -> {
                                Log.i("Type_RESPONSE", "Loading.. onViewCreated: ${it.message}")
                            }

                            is Response.Success -> {
                                Log.i("Type_RESPONSE", "Success.. onViewCreated: ${it.data}")
                                when (it.data.connectionState) {
                                    ConnectionState.Connected -> {
                                        Log.i(
                                            "Type_RESPONSE",
                                            "Success.. {Testing} onViewCreated: Connection"
                                        )
                                    }

                                    ConnectionState.DisConnected -> {
                                        Log.i(
                                            "Type_RESPONSE",
                                            "Success.. {Testing} onViewCreated: DisConnected"
                                        )
                                    }

                                    ConnectionState.Initialized -> {
                                        Log.i(
                                            "Type_RESPONSE",
                                            "Success.. {Testing} onViewCreated: Initialized "
                                        )
                                    }

                                    ConnectionState.UnInitialized -> {
                                        Log.i(
                                            "Type_RESPONSE",
                                            "Success.. {Testing} onViewCreated: Initialized "
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bluetoothReg?.registerReceiver(BluetoothAdapter.ACTION_STATE_CHANGED)
    }

    override fun onPause() {
        super.onPause()
        bluetoothReg?.unregisterReceiver()
    }

}