package com.example.testingbl.ui

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.testingbl.R
import com.example.testingbl.databinding.TemperatureLayoutBinding
import com.example.testingbl.utils.BlueToothBroadCastReceiver


class TemperatureScreenFragment(private val enableBlue: () -> Unit) :
    Fragment(R.layout.temperature_layout) {

    private lateinit var binding: TemperatureLayoutBinding

    private var bluetoothReg: BlueToothBroadCastReceiver? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = TemperatureLayoutBinding.bind(view)
        bluetoothReg = BlueToothBroadCastReceiver(requireActivity()) {
            if (it.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                enableBlue.invoke()
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