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

@Database(entities = [LyricsEntity::class], version = 2, exportSchema = false)
abstract class VinylDatabase : RoomDatabase() {
    abstract fun lyricsDao(): LyricsDao

    companion object {
        @Volatile private var instance: VinylDatabase? = null

        fun get(context: Context): VinylDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VinylDatabase::class.java,
                "vinylplayer.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lyrics ADD COLUMN translatedLyrics TEXT")
                database.execSQL("ALTER TABLE lyrics ADD COLUMN translatedSyncedLyrics TEXT")
            }
        }
    }
}
