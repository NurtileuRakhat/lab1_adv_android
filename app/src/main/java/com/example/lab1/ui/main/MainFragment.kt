package com.example.lab1.ui.main

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lab1.R
import com.example.lab1.data.service.AirplaneModeReceiver
import com.example.lab1.databinding.FragmentMainBinding

class MainFragment : Fragment(), AirplaneModeReceiver.AirplaneModeListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val airplaneModeReceiver = AirplaneModeReceiver()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupAirplaneModeReceiver()
        updateAirplaneModeButton()
    }

    private fun setupClickListeners() {
        binding.apply {
            musicCard.setOnClickListener {
                findNavController().navigate(R.id.musicFragment)
            }

            calendarCard.setOnClickListener {
                findNavController().navigate(R.id.calendarFragment)
            }

            instagramCard.setOnClickListener {
                findNavController().navigate(R.id.instagramFragment)
            }

            btnAirplaneMode.setOnClickListener {
                toggleAirplaneMode()
            }
        }
    }

    private fun setupAirplaneModeReceiver() {
        airplaneModeReceiver.setListener(this)
        requireContext().registerReceiver(
            airplaneModeReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        )
    }

    private fun updateAirplaneModeButton() {
        val isAirplaneModeOn = AirplaneModeReceiver.isAirplaneModeOn(requireContext())
        binding.btnAirplaneMode.text = if (isAirplaneModeOn) {
            "Выключить режим полета"
        } else {
            "Включить режим полета"
        }
    }

    private fun toggleAirplaneMode() {
        val isCurrentlyOn = AirplaneModeReceiver.isAirplaneModeOn(requireContext())
        AirplaneModeReceiver.setAirplaneMode(requireContext(), !isCurrentlyOn)
    }

    override fun onAirplaneModeChanged(isEnabled: Boolean) {
        updateAirplaneModeButton()
    }

    override fun onResume() {
        super.onResume()
        updateAirplaneModeButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            airplaneModeReceiver.removeListener()
            requireContext().unregisterReceiver(airplaneModeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _binding = null
    }
} 