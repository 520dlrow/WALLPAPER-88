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
            val bitmap = downloadRandomImage() ?: return@withContext Result.retry()
            val wm = WallpaperManager.getInstance(applicationContext)
            val targetW = wm.desiredMinimumWidth.takeIf { it > 0 } ?: bitmap.width
            val targetH = wm.desiredMinimumHeight.takeIf { it > 0 } ?: bitmap.height
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            wm.setBitmap(scaled, null, true, WallpaperManager.FLAG_SYSTEM)
            wm.setBitmap(scaled, null, true, WallpaperManager.FLAG_LOCK)
            NotificationHelper.showWallpaperChanged(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun downloadRandomImage(): Bitmap? {
        val url = URL(IMAGE_ENDPOINT)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        connection.connect()
        return connection.inputStream.use { BitmapFactory.decodeStream(it) }
    }

    companion object {
        private const val IMAGE_ENDPOINT = "https://picsum.photos/1080/1920"
    }
}
