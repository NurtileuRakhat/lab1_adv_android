package com.example.lab1.ui.instagram

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lab1.BuildConfig
import com.example.lab1.databinding.FragmentInstagramBinding
import java.io.File
import java.io.FileOutputStream

class InstagramFragment : Fragment() {
    private var _binding: FragmentInstagramBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Необходимы разрешения для доступа к фото", Toast.LENGTH_LONG).show()
        }
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imageView.setImageURI(it)
            binding.btnShare.isEnabled = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstagramBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        binding.btnShare.setOnClickListener {
            shareToInstagramStories()
        }

        binding.btnShare.isEnabled = false
    }

    private fun checkPermissionAndOpenGallery() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun openGallery() {
        pickImage.launch("image/*")
    }

    private fun shareToInstagramStories() {
        selectedImageUri?.let { uri ->
            val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "image/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                startActivity(storiesIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Instagram не установлен или нет доступа",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 