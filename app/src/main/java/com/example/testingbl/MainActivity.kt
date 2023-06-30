package com.example.testingbl

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import com.example.testingbl.databinding.ActivityMainBinding
import com.example.testingbl.ui.StartScreenFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val fragTrans = supportFragmentManager.beginTransaction()
        fragTrans.replace(binding.frgTxt.id, StartScreenFragment())
        fragTrans.commit()
    }
}