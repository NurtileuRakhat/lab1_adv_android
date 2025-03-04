package com.example.lab1.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

class AirplaneModeReceiver : BroadcastReceiver() {
    private var listener: AirplaneModeListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            val isAirplaneModeOn = Settings.Global.getInt(
                context?.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0

            // Показываем Toast сообщение
            val message = if (isAirplaneModeOn) {
                "Режим полета включен"
            } else {
                "Режим полета выключен"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

            // Уведомляем слушателя об изменении состояния
            listener?.onAirplaneModeChanged(isAirplaneModeOn)
        }
    }

    fun setListener(listener: AirplaneModeListener) {
        this.listener = listener
    }

    fun removeListener() {
        listener = null
    }

    interface AirplaneModeListener {
        fun onAirplaneModeChanged(isEnabled: Boolean)
    }

    companion object {
        fun isAirplaneModeOn(context: Context): Boolean {
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        }

        fun setAirplaneMode(context: Context, enable: Boolean) {
            try {
                Settings.Global.putInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    if (enable) 1 else 0
                )

                // Отправляем broadcast для обновления состояния
                val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                context.sendBroadcast(intent)
            } catch (e: SecurityException) {
                // Если нет прав на прямое изменение, открываем настройки
                val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
    }
} 