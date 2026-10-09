package dev.cipher.notes.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.cipher.notes.crypto.DataStoreLockoutStateStore
import dev.cipher.notes.crypto.LockoutController
import dev.cipher.notes.data.NoteDao
import dev.cipher.notes.data.NoteDatabase
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): NoteDatabase =
        Room.databaseBuilder(ctx, NoteDatabase::class.java, "cipher_notes.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideNoteDao(db: NoteDatabase): NoteDao = db.noteDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            // A damaged settings file used to throw on every read, which left
            // the startup gate waiting forever and then showed a lock screen
            // whose PIN lives in that same unreadable file: an unpassable wall.
            // Falling back to a clean slate opens the app with the lock off and
            // default settings; notes live in Room, so they are unaffected.
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }

    @Provides @Singleton @Named("lockoutDataStore")
    fun provideLockoutDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            // A damaged lockout file must never block unlocking a note: fall
            // back to a clean slate rather than throwing on read.
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("note_lockout") }
        )
    }

    @Provides @Singleton
    fun provideLockoutController(store: DataStoreLockoutStateStore): LockoutController =
        LockoutController(store)
}
