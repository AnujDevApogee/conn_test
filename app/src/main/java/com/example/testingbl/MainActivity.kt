package com.example.testingbl

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.example.testingbl.databinding.ActivityMainBinding
import com.example.testingbl.di.BluetoothCommunication
import com.example.testingbl.ui.StartScreenFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    private val bluTohAdaptor by lazy {
        BluetoothCommunication.getInstance(this).getBluetoothAdaptor()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val fragTrans = supportFragmentManager.beginTransaction()
        fragTrans.replace(binding.frgTxt.id, StartScreenFragment{
            showBluetoothEnableDialog()
        })
        fragTrans.commit()
    }


    override fun onStart() {
        super.onStart()
        showBluetoothEnableDialog()
    }

    private fun showBluetoothEnableDialog() {
        if (!bluTohAdaptor.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            registerActivity.launch(intent)
        }
    }


    private val registerActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) {
                showBluetoothEnableDialog()
            }
        }


}