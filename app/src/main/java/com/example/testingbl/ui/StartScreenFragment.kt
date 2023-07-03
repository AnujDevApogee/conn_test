package com.example.testingbl.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.testingbl.R
import com.example.testingbl.databinding.StartScreenLayoutBinding
import com.example.testingbl.utils.PermissionUtils
import com.permissionx.guolindev.PermissionX


class StartScreenFragment(private val enableBlue: () -> Unit) :
    Fragment(R.layout.start_screen_layout) {
    private lateinit var binding: StartScreenLayoutBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = StartScreenLayoutBinding.bind(view)
        binding.startBtn.setOnClickListener {
            goToTemp()
        }

        PermissionX.init(requireActivity())
            .permissions(PermissionUtils.permissions)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Toast.makeText(
                        requireActivity(),
                        "All permissions are granted",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireActivity(),
                        "These permissions are denied: $deniedList",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun goToTemp() {
        val fragment: Fragment = TemperatureScreenFragment {
            enableBlue.invoke()
        }
        val fragmentManager = requireActivity().supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frg_txt, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

}