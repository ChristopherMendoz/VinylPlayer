package com.chrismdz.vinylplayer.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(
    tableName = "lyrics",
    indices = [Index(value = ["songId"], unique = true)]
)
data class LyricsEntity(
    @PrimaryKey val songId: String,
    val lyrics: String?,
    val syncedLyrics: String?,
    val translatedLyrics: String? = null,
    val translatedSyncedLyrics: String? = null,
    val translationLanguage: String? = null,
    val source: String?,
    val fetchedAt: Long
)

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE songId = :songId LIMIT 1")
    suspend fun find(songId: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(lyrics: LyricsEntity)
}

@Database(entities = [LyricsEntity::class, ArtistEntity::class, ArtistTrackCreditEntity::class], version = 4, exportSchema = false)
abstract class VinylDatabase : RoomDatabase() {
    abstract fun lyricsDao(): LyricsDao
    abstract fun artistDao(): ArtistDao

    companion object {
        @Volatile private var instance: VinylDatabase? = null

        fun get(context: Context): VinylDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VinylDatabase::class.java,
                "vinylplayer.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lyrics ADD COLUMN translatedLyrics TEXT")
                database.execSQL("ALTER TABLE lyrics ADD COLUMN translatedSyncedLyrics TEXT")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lyrics ADD COLUMN translationLanguage TEXT")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS artists (artistId TEXT NOT NULL, name TEXT NOT NULL, imageUrl TEXT, imageCheckedAt INTEGER, PRIMARY KEY(artistId))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS artist_track_credits (artistId TEXT NOT NULL, songId TEXT NOT NULL, position INTEGER NOT NULL, role TEXT NOT NULL, PRIMARY KEY(artistId, songId), FOREIGN KEY(artistId) REFERENCES artists(artistId) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_artist_track_credits_songId ON artist_track_credits(songId)"
                )
            }
        }
    }
}
