package com.example.testingbl.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.testingbl.R
import com.example.testingbl.databinding.StartScreenLayoutBinding


class StartScreenFragment(private val enableBlue: () -> Unit) : Fragment(R.layout.start_screen_layout) {
    private lateinit var binding: StartScreenLayoutBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = StartScreenLayoutBinding.bind(view)
        binding.startBtn.setOnClickListener {
            goToTemp()
        }
    }

    private fun goToTemp() {
        val fragment: Fragment = TemperatureScreenFragment{
            enableBlue.invoke()
        }
        val fragmentManager = requireActivity().supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frg_txt, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

}