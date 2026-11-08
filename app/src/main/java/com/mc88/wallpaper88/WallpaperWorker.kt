package com.mc88.wallpaper88

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class WallpaperWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("Wallpaper88Prefs", Context.MODE_PRIVATE)
            val customUrl = prefs.getString("custom_url", "")?.trim()
            val setHome = prefs.getBoolean("set_home", true)
            val setLock = prefs.getBoolean("set_lock", true)

            val targetUrl = if (customUrl.isNullOrEmpty()) DEFAULT_IMAGE else customUrl

            val bitmap = downloadImage(targetUrl) ?: return@withContext Result.retry()
            
            val wm = WallpaperManager.getInstance(applicationContext)
            val targetW = wm.desiredMinimumWidth.takeIf { it > 0 } ?: bitmap.width
            val targetH = wm.desiredMinimumHeight.takeIf { it > 0 } ?: bitmap.height
            
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            
            if (setHome) {
                wm.setBitmap(scaled, null, true, WallpaperManager.FLAG_SYSTEM)
            }
            if (setLock) {
                wm.setBitmap(scaled, null, true, WallpaperManager.FLAG_LOCK)
            }
            
            NotificationHelper.showWallpaperChanged(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun downloadImage(urlString: String): Bitmap? {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        connection.connect()
        return connection.inputStream.use { BitmapFactory.decodeStream(it) }
    }

    companion object {
        private const val DEFAULT_IMAGE = "https://picsum.photos/1080/1920"
    }
}
