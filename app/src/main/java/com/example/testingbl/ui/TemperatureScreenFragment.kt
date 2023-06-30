package com.example.testingbl.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.testingbl.R
import com.example.testingbl.databinding.TemperatureLayoutBinding

class TemperatureScreenFragment : Fragment(R.layout.temperature_layout) {
    private lateinit var binding: TemperatureLayoutBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = TemperatureLayoutBinding.bind(view)

    }
}