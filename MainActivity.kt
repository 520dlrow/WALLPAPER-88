package com.mc88.wallpaper88

import android.Manifest
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var intervalGroup: RadioGroup

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        intervalGroup = findViewById(R.id.intervalGroup)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        findViewById<RadioButton>(R.id.radio30m).isChecked = true

        findViewById<Button>(R.id.startButton).setOnClickListener {
            val minutes = when (intervalGroup.checkedRadioButtonId) {
                R.id.radio30m -> 30L
                R.id.radio6h -> 360L
                R.id.radio1d -> 1440L
                else -> 30L
            }
            scheduleWorker(minutes)
            statusText.text = getString(R.string.status_running, formatInterval(minutes))
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            WorkManager.getInstance(this).cancelUniqueWork(WORK_NAME)
            statusText.text = getString(R.string.status_stopped)
        }

        val wm = WallpaperManager.getInstance(this)
        val w = wm.desiredMinimumWidth
        val h = wm.desiredMinimumHeight
        if (w > 0 && h > 0) {
            findViewById<TextView>(R.id.screenInfo).text =
                getString(R.string.screen_info, w, h)
        }
    }

    private fun scheduleWorker(intervalMinutes: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<WallpaperWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun formatInterval(minutes: Long): String = when (minutes) {
        30L -> getString(R.string.interval_30m)
        360L -> getString(R.string.interval_6h)
        else -> getString(R.string.interval_1d)
    }

    companion object {
        const val WORK_NAME = "wallpaper88_worker"
    }
}
