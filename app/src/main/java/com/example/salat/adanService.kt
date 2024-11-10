package com.example.salat

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class AdhanService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val prayerName = intent.getStringExtra("prayerName") ?: return START_NOT_STICKY


        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                release()
            }
        }


        val adhanResource = when (prayerName) {
            "Fajr" -> R.raw.adan
            "Dhuhr" -> R.raw.adan
            "Asr" -> R.raw.adan
            "Maghrib" -> R.raw.adan
            "Isha" -> R.raw.adan
            else -> return START_NOT_STICKY
        }

        mediaPlayer = MediaPlayer.create(this, adhanResource)
        mediaPlayer?.setOnCompletionListener {

            mediaPlayer?.release()
        }
        mediaPlayer?.start()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}
