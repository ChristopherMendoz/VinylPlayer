package com.chrismdz.vinylplayer.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ArtistImageWorkScheduler(
    context: Context,
    private val artistDao: ArtistDao
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val schedulingMutex = Mutex()

    suspend fun cancelPendingPrefetch() = withContext(Dispatchers.IO) {
        schedulingMutex.withLock {
            artistDao.findPendingArtistImageWork().forEach { artist ->
                workManager.cancelUniqueWork(workName(artist.artistId))
            }
            artistDao.resetPendingArtistImageWork()
        }
    }

    suspend fun scheduleEligibleArtists() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        artistDao.findArtistsNeedingImageWork(now).forEach { artist ->
            enqueue(artist.artistId, force = false, now = now)
        }
    }

    suspend fun scheduleArtist(artistId: String, force: Boolean = false) = withContext(Dispatchers.IO) {
        schedulingMutex.withLock {
            enqueue(artistId, force, System.currentTimeMillis())
        }
    }

    private suspend fun enqueue(artistId: String, force: Boolean, now: Long) {
        val artist = artistDao.findArtist(artistId) ?: return
        if (!force && artist.imageStatus == ArtistImageStatus.DOWNLOADED) return
        if (!force && artist.imageStatus in setOf(ArtistImageStatus.ERROR, ArtistImageStatus.NOT_FOUND) &&
            artist.imageRetryAfter?.let { it > now } == true
        ) return

        artistDao.updateImageStatus(artistId, ArtistImageStatus.PENDING, null, null)
        val request = OneTimeWorkRequestBuilder<ArtistImageWorker>()
            .setInputData(workDataOf(ArtistImageWorker.ARTIST_ID_KEY to artistId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, FIRST_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(workName(artistId), ExistingWorkPolicy.KEEP, request)
    }

    private fun workName(artistId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(artistId.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return "$WORK_NAME_PREFIX$digest"
    }

    private companion object {
        const val WORK_NAME_PREFIX = "artist-image-"
        const val FIRST_BACKOFF_MINUTES = 15L
    }
}
