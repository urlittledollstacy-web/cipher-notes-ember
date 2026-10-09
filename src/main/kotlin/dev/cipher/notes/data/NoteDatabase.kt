package dev.cipher.notes.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        /**
         * Adds the ciphertext integrity column.
         *
         * The column is nullable and left NULL for existing rows: their notes are
         * unreadable otherwise, so they skip the integrity check rather than being
         * flagged as damaged. Declared explicitly because the database builder
         * falls back to destructive migration, which would otherwise delete every
         * note on this upgrade.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No DEFAULT clause: the column is nullable and Room appends an
                // explicit value on every insert. `DEFAULT NULL` would be recorded
                // as the literal string 'NULL', which is not what Room expects.
                db.execSQL("ALTER TABLE notes ADD COLUMN ciphertextHash TEXT")
            }
        }
    }
}
