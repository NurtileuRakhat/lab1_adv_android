package com.example.lab1.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.example.lab1.MainActivity
import com.example.lab1.R

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val CHANNEL_ID = "MusicServiceChannel"
    private val NOTIFICATION_ID = 1
    private var isPlaying = false
    private lateinit var mediaSession: MediaSessionCompat
    private var notificationManager: NotificationManager? = null
    private lateinit var audioManager: AudioManager
    private val audioFocusRequest: AudioFocusRequest? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        } else null
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pauseMusic()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pauseMusic()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                if (!isPlaying) {
                    playMusic()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initializeMediaPlayer()
        initializeMediaSession()
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playMusic()
                }
                override fun onPause() {
                    pauseMusic()
                }
                override fun onStop() {
                    stopMusic()
                    stopForeground(true)
                    stopSelf()
                }
            })

            setMetadata(MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Sample Music")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Sample Artist")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1L)
                .build())

            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 1f)
            setPlaybackState(stateBuilder.build())
            
            isActive = true
        }
    }

    private fun updatePlaybackState(state: Int) {
        if (!::mediaSession.isInitialized) return
        
        try {
            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
            
            val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
            stateBuilder.setState(state, position, if (isPlaying) 1f else 0f)
            mediaSession.setPlaybackState(stateBuilder.build())
            updateNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNotification() {
        try {
            val notification = createNotification()
            if (isPlaying) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                stopForeground(false)
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeMediaPlayer() {
        try {
            releaseMediaPlayer()
            
            mediaPlayer = MediaPlayer().apply {
                assets.openFd("sample_music.mp3").use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                setOnPreparedListener {
                    it.setVolume(1.0f, 1.0f)
                }
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(this@MusicService, "Ошибка воспроизведения: $what", Toast.LENGTH_SHORT).show()
                    stopSelf()
                    true
                }
                setOnCompletionListener {
                    stopMusic()
                    stopForeground(true)
                    stopSelf()
                }
                prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка инициализации плеера: ${e.message}", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Канал для музыкального сервиса"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        if (!::mediaSession.isInitialized) {
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Музыкальный плеер")
                .setSmallIcon(R.drawable.ic_music)
                .build()
        }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = NotificationCompat.Action.Builder(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying) "Пауза" else "Играть",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
            )
        ).build()

        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stop,
            "Стоп",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_STOP
            )
        ).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(mediaStyle)
            .setContentTitle("Sample Music")
            .setContentText("Sample Artist")
            .setSmallIcon(R.drawable.ic_music)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (::mediaSession.isInitialized) {
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
            
            when (intent?.action) {
                ACTION_PLAY -> playMusic()
                ACTION_PAUSE -> pauseMusic()
                ACTION_STOP -> {
                    stopMusic()
                    stopForeground(true)
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_NOT_STICKY
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager.requestAudioFocus(request)
            } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun playMusic() {
        try {
            if (!isPlaying && mediaPlayer != null) {
                if (requestAudioFocus()) {
                    if (mediaPlayer?.isPlaying == false) {
                        try {
                            mediaPlayer?.start()
                            isPlaying = true
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        } catch (e: IllegalStateException) {
                            initializeMediaPlayer()
                            mediaPlayer?.start()
                            isPlaying = true
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun pauseMusic() {
        try {
            if (isPlaying && mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == true) {
                    try {
                        mediaPlayer?.pause()
                        isPlaying = false
                        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        abandonAudioFocus()
                    } catch (e: IllegalStateException) {
                        isPlaying = false
                        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        abandonAudioFocus()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMusic() {
        try {
            if (mediaPlayer?.isPlaying() == true) {
                try {
                    mediaPlayer?.stop()
                } catch (e: IllegalStateException) {
                    // Игнорируем ошибку остановки
                }
            }
            abandonAudioFocus()
            initializeMediaPlayer()
            isPlaying = false
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        try {
            if (::mediaSession.isInitialized) {
                mediaSession.isActive = false
                mediaSession.release()
            }
            abandonAudioFocus()
            stopForeground(true)
            releaseMediaPlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_PLAY = "PLAY"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_STOP = "STOP"
    }
} 