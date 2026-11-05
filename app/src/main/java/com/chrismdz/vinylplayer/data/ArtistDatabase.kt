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
    val imageCheckedAt: Long? = null
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
    val imageCheckedAt: Long?
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
            artists.imageUrl AS imageUrl, artists.imageCheckedAt AS imageCheckedAt
            FROM artists INNER JOIN artist_track_credits
            ON artists.artistId = artist_track_credits.artistId
            GROUP BY artists.artistId"""
    )
    fun observeCatalog(): Flow<List<ArtistCatalogItem>>

    @Query("SELECT songId, position, role FROM artist_track_credits WHERE artistId = :artistId ORDER BY position ASC")
    fun observeTrackLinks(artistId: String): Flow<List<ArtistTrackLink>>

    @Query("SELECT * FROM artists WHERE artistId = :artistId LIMIT 1")
    suspend fun findArtist(artistId: String): ArtistEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM artist_track_credits")
    suspend fun clearTrackLinks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackLinks(links: List<ArtistTrackCreditEntity>)

    @Query("DELETE FROM artists WHERE artistId NOT IN (SELECT DISTINCT artistId FROM artist_track_credits)")
    suspend fun deleteUnusedArtists()

    @Query("UPDATE artists SET imageUrl = :imageUrl, imageCheckedAt = :checkedAt WHERE artistId = :artistId")
    suspend fun updateImageLookup(artistId: String, imageUrl: String?, checkedAt: Long)

    @Transaction
    suspend fun refreshLibrary(artists: List<ArtistEntity>, links: List<ArtistTrackCreditEntity>) {
        insertArtists(artists)
        clearTrackLinks()
        insertTrackLinks(links)
        deleteUnusedArtists()
    }
}
