package com.mc88.wallpaper88

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var intervalGroup: RadioGroup
    private lateinit var apiInput: EditText
    private lateinit var checkHome: CheckBox
    private lateinit var checkLock: CheckBox

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        intervalGroup = findViewById(R.id.intervalGroup)
        apiInput = findViewById(R.id.apiInput)
        checkHome = findViewById(R.id.checkHome)
        checkLock = findViewById(R.id.checkLock)

        loadSettings()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (!checkHome.isChecked && !checkLock.isChecked) {
                Toast.makeText(this, R.string.status_no_screen, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveSettings()
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

        findViewById<Button>(R.id.changeNowButton).setOnClickListener {
            if (!checkHome.isChecked && !checkLock.isChecked) {
                Toast.makeText(this, R.string.status_no_screen, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveSettings()
            statusText.text = getString(R.string.status_changing_now)

            val request = OneTimeWorkRequestBuilder<WallpaperWorker>().build()
            WorkManager.getInstance(this).enqueue(request)

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id)
                .observe(this) { workInfo ->
                    if (workInfo != null && workInfo.state.isFinished) {
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            statusText.text = "Success! Wallpaper changed."
                        } else if (workInfo.state == WorkInfo.State.FAILED) {
                            statusText.text = "Failed. Check URL or Internet."
                        }
                    }
                }
        }

        try {
            val wm = WallpaperManager.getInstance(this)
            val w = wm.desiredMinimumWidth
            val h = wm.desiredMinimumHeight
            if (w > 0 && h > 0) {
                findViewById<TextView>(R.id.screenInfo).text =
                    getString(R.string.screen_info, w, h)
            } else {
                findViewById<TextView>(R.id.screenInfo).text = "Screen target: Auto"
            }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.screenInfo).text = "Screen target: Auto"
        }
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("Wallpaper88Prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("custom_url", apiInput.text.toString().trim())
            .putBoolean("set_home", checkHome.isChecked)
            .putBoolean("set_lock", checkLock.isChecked)
            .apply()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("Wallpaper88Prefs", Context.MODE_PRIVATE)
        apiInput.setText(prefs.getString("custom_url", ""))
        checkHome.isChecked = prefs.getBoolean("set_home", true)
        checkLock.isChecked = prefs.getBoolean("set_lock", true)
        findViewById<RadioButton>(R.id.radio30m).isChecked = true
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
