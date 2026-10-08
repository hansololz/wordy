package com.deezus.wordy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
  entities = [GameRecordEntity::class, BookmarkEntity::class, UsedWordEntity::class],
  version = 1,
  exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WordyDatabase : RoomDatabase() {
  abstract fun gameRecordDao(): GameRecordDao
  abstract fun bookmarkDao(): BookmarkDao
  abstract fun usedWordDao(): UsedWordDao

  companion object {
    const val NAME = "wordy.db"

    fun create(context: Context): WordyDatabase =
      Room.databaseBuilder(context, WordyDatabase::class.java, NAME)
        // Auto Backup copies only wordy.db, not the write-ahead log. With WAL the main file can
        // lag behind by months for a database this small, so a restore would miss recent games.
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        .build()
  }
}

class Converters {
  // Words only contain letters, so a comma is a safe separator.
  @TypeConverter
  fun guessesToString(guesses: List<String>): String = guesses.joinToString(",")

  @TypeConverter
  fun stringToGuesses(value: String): List<String> =
    if (value.isEmpty()) emptyList() else value.split(",")
}
