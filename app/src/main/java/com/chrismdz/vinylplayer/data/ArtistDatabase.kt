package com.chrismdz.vinylplayer.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "artists")
data class ArtistEntity(
    @androidx.room.PrimaryKey val artistId: String,
    val name: String,
    val imageUrl: String? = null,
    val imageCheckedAt: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "'NOT_DOWNLOADED'")
    val imageStatus: String = ArtistImageStatus.NOT_DOWNLOADED,
    val imageSource: String? = null,
    val imageLicense: String? = null,
    val imageAttribution: String? = null,
    val imagePageUrl: String? = null,
    val imageRetryAfter: Long? = null,
    val imageError: String? = null
)

@Entity(
    tableName = "artist_track_credits",
    primaryKeys = ["artistId", "songId"],
    indices = [Index(value = ["songId"])],
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["artistId"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ArtistTrackCreditEntity(
    val artistId: String,
    val songId: String,
    val position: Int,
    val role: String
)

data class ArtistCatalogItem(
    val artistId: String,
    val name: String,
    val songCount: Int,
    val imageUrl: String?,
    val imageCheckedAt: Long?,
    val imageStatus: String = ArtistImageStatus.NOT_DOWNLOADED,
    val imageSource: String? = null,
    val imageLicense: String? = null,
    val imageAttribution: String? = null,
    val imagePageUrl: String? = null,
    val imageRetryAfter: Long? = null,
    val imageError: String? = null
)

data class ArtistTrackLink(
    val songId: String,
    val position: Int,
    val role: String
)

@Dao
interface ArtistDao {
    @Query(
        """SELECT artists.artistId AS artistId, artists.name AS name,
            COUNT(DISTINCT artist_track_credits.songId) AS songCount,
            artists.imageUrl AS imageUrl, artists.imageCheckedAt AS imageCheckedAt,
            artists.imageStatus AS imageStatus, artists.imageSource AS imageSource,
            artists.imageLicense AS imageLicense, artists.imageAttribution AS imageAttribution,
            artists.imagePageUrl AS imagePageUrl, artists.imageRetryAfter AS imageRetryAfter,
            artists.imageError AS imageError
            FROM artists INNER JOIN artist_track_credits
            ON artists.artistId = artist_track_credits.artistId
            GROUP BY artists.artistId"""
    )
    fun observeCatalog(): Flow<List<ArtistCatalogItem>>

    @Query("SELECT songId, position, role FROM artist_track_credits WHERE artistId = :artistId ORDER BY position ASC")
    fun observeTrackLinks(artistId: String): Flow<List<ArtistTrackLink>>

    @Query("SELECT * FROM artists WHERE artistId = :artistId LIMIT 1")
    suspend fun findArtist(artistId: String): ArtistEntity?

    @Query("""SELECT * FROM artists WHERE
        imageStatus IN ('NOT_DOWNLOADED', 'PENDING', 'DOWNLOADING') OR
        (imageStatus IN ('NOT_FOUND', 'ERROR') AND (imageRetryAfter IS NULL OR imageRetryAfter <= :now))""")
    suspend fun findArtistsNeedingImageWork(now: Long): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE imageStatus IN ('PENDING', 'DOWNLOADING')")
    suspend fun findPendingArtistImageWork(): List<ArtistEntity>

    @Query("UPDATE artists SET imageStatus = 'NOT_DOWNLOADED', imageError = NULL, imageRetryAfter = NULL WHERE imageStatus IN ('PENDING', 'DOWNLOADING')")
    suspend fun resetPendingArtistImageWork()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM artist_track_credits")
    suspend fun clearTrackLinks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackLinks(links: List<ArtistTrackCreditEntity>)

    @Query("DELETE FROM artists WHERE artistId NOT IN (SELECT DISTINCT artistId FROM artist_track_credits)")
    suspend fun deleteUnusedArtists()

    @Query("UPDATE artists SET imageStatus = :status, imageError = :error, imageRetryAfter = :retryAfter WHERE artistId = :artistId")
    suspend fun updateImageStatus(artistId: String, status: String, error: String?, retryAfter: Long?)

    @Query("UPDATE artists SET imageStatus = :status, imageCheckedAt = :checkedAt, imageError = :error, imageRetryAfter = :retryAfter WHERE artistId = :artistId")
    suspend fun updateImageResult(artistId: String, status: String, checkedAt: Long, error: String?, retryAfter: Long?)

    @Query("""UPDATE artists SET imageUrl = :imageUrl, imageCheckedAt = :checkedAt,
        imageStatus = 'DOWNLOADED', imageSource = :source, imageLicense = :license,
        imageAttribution = :attribution, imagePageUrl = :pageUrl,
        imageRetryAfter = NULL, imageError = NULL WHERE artistId = :artistId""")
    suspend fun updateDownloadedImage(
        artistId: String,
        imageUrl: String,
        checkedAt: Long,
        source: String,
        license: String?,
        attribution: String?,
        pageUrl: String?
    )

    @Transaction
    suspend fun refreshLibrary(artists: List<ArtistEntity>, links: List<ArtistTrackCreditEntity>) {
        insertArtists(artists)
        clearTrackLinks()
        insertTrackLinks(links)
        deleteUnusedArtists()
    }
}
