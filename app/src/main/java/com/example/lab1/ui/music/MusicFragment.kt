package com.example.lab1.ui.music

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lab1.R
import com.example.lab1.databinding.FragmentMusicBinding
import com.example.lab1.services.MusicService

class MusicFragment : Fragment() {
    private var _binding: FragmentMusicBinding? = null
    private val binding get() = _binding!!
    private var isPlaying = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) {
                pauseMusic()
            } else {
                playMusic()
            }
        }

        binding.btnStop.setOnClickListener {
            stopMusic()
        }
    }

    private fun playMusic() {
        Intent(requireContext(), MusicService::class.java).also { intent ->
            intent.action = MusicService.ACTION_PLAY
            requireContext().startService(intent)
        }
        isPlaying = true
        updateUI()
    }

    private fun pauseMusic() {
        Intent(requireContext(), MusicService::class.java).also { intent ->
            intent.action = MusicService.ACTION_PAUSE
            requireContext().startService(intent)
        }
        isPlaying = false
        updateUI()
    }

    private fun stopMusic() {
        Intent(requireContext(), MusicService::class.java).also { intent ->
            intent.action = MusicService.ACTION_STOP
            requireContext().startService(intent)
        }
        isPlaying = false
        updateUI()
    }

    private fun updateUI() {
        binding.btnPlayPause.apply {
            text = if (isPlaying) "Пауза" else "Играть"
            setIconResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        }
        binding.musicStatus.text = if (isPlaying) "Воспроизведение" else "Остановлено"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 