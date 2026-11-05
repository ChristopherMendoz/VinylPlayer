package com.chrismdz.vinylplayer.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.concurrent.TimeUnit

class ArtistImageWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val artistId = inputData.getString(ARTIST_ID_KEY) ?: return Result.failure()
        val database = VinylDatabase.get(applicationContext)
        return try {
            val repository = ArtistImageRepository(
                artistDao = database.artistDao(),
                providers = listOf(
                    DeezerArtistImageProvider(),
                    MusicBrainzWikidataArtistImageProvider(applicationContext)
                ),
                imageDirectory = File(applicationContext.filesDir, "artist-images")
            )
            when (repository.processArtistImage(artistId, runAttemptCount)) {
                ArtistImageWorkResult.COMPLETE -> Result.success()
                ArtistImageWorkResult.RETRY -> Result.retry()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            val now = System.currentTimeMillis()
            val retryMinutes = minOf(24 * 60, 15 * (1 shl runAttemptCount.coerceAtMost(10)))
            database.artistDao().updateImageResult(
                artistId,
                ArtistImageStatus.ERROR,
                now,
                "WORKER_ERROR",
                now + TimeUnit.MINUTES.toMillis(retryMinutes.toLong())
            )
            Result.retry()
        }
    }

    companion object {
        const val ARTIST_ID_KEY = "artist_id"
    }
}
