package com.example.lab1.ui.calendar

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lab1.data.model.CalendarEvent
import com.example.lab1.databinding.FragmentCalendarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CalendarFragment : Fragment() {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CalendarEventAdapter

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            loadCalendarEvents()
        } else {
            Toast.makeText(requireContext(), "Необходимы разрешения для работы с календарем", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        checkPermissionsAndLoadEvents()
    }

    private fun setupRecyclerView() {
        adapter = CalendarEventAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CalendarFragment.adapter
        }
    }

    private fun checkPermissionsAndLoadEvents() {
        when {
            hasCalendarPermissions() -> loadCalendarEvents()
            else -> requestCalendarPermissions()
        }
    }

    private fun hasCalendarPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCalendarPermissions() {
        calendarPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun loadCalendarEvents() {
        showLoading(true)
        
        // Используем корутину для загрузки событий в фоновом потоке
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) {
                    fetchCalendarEvents()
                }

                if (events.isEmpty()) {
                    // Если событий нет, добавляем тестовые данные
                    withContext(Dispatchers.IO) {
                        addTestEvents()
                    }
                    // Загружаем события снова
                    val updatedEvents = withContext(Dispatchers.IO) {
                        fetchCalendarEvents()
                    }
                    updateUI(updatedEvents)
                } else {
                    updateUI(events)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Ошибка при загрузке событий: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun fetchCalendarEvents(): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val currentTime = System.currentTimeMillis()
        val endTime = currentTime + (7 * 24 * 60 * 60 * 1000) // неделя вперед

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(currentTime.toString(), endTime.toString())

        requireContext().contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val title = cursor.getString(1) ?: "Без названия"
                val description = cursor.getString(2) ?: "Без описания"
                val startDate = Date(cursor.getLong(3))
                val endDate = Date(cursor.getLong(4))

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        description = description,
                        startDate = startDate,
                        endDate = endDate
                    )
                )
            }
        }

        return events
    }

    private fun addTestEvents() {
        val calendarId = getDefaultCalendarId()
        if (calendarId == -1L) {
            throw IllegalStateException("Календарь по умолчанию не найден")
        }

        val currentTime = System.currentTimeMillis()
        val testEvents = listOf(
            Triple(
                "Встреча с командой",
                "Обсуждение проекта",
                Pair(currentTime + 24 * 60 * 60 * 1000, currentTime + 25 * 60 * 60 * 1000)
            ),
            Triple(
                "Презентация",
                "Демонстрация новых функций",
                Pair(currentTime + 48 * 60 * 60 * 1000, currentTime + 49 * 60 * 60 * 1000)
            ),
            Triple(
                "Тренировка",
                "Спортзал",
                Pair(currentTime + 72 * 60 * 60 * 1000, currentTime + 73 * 60 * 60 * 1000)
            )
        )

        testEvents.forEach { (title, description, time) ->
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.DTSTART, time.first)
                put(CalendarContract.Events.DTEND, time.second)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            requireContext().contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        }
    }

    private fun getDefaultCalendarId(): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        
        requireContext().contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        
        return -1L
    }

    private fun updateUI(events: List<CalendarEvent>) {
        if (events.isEmpty()) {
            showEmptyState()
        } else {
            showEvents(events)
        }
    }

    private fun showEvents(events: List<CalendarEvent>) {
        binding.recyclerView.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
        adapter.submitList(events)
    }

    private fun showEmptyState() {
        binding.recyclerView.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    }
}