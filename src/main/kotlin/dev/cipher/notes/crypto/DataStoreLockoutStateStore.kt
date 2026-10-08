package dev.cipher.notes.crypto

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named

/**
 * Persists each note's lockout state in its own preferences file.
 *
 * Deliberately not the settings file: lock state and user settings fail
 * independently, and a settings reset must never be able to clear a lockout.
 */
class DataStoreLockoutStateStore @Inject constructor(
    @Named("lockoutDataStore") private val dataStore: DataStore<Preferences>
) : LockoutStateStore {

    private fun attemptsKey(noteId: String) = intPreferencesKey("attempts_$noteId")
    private fun untilKey(noteId: String) = longPreferencesKey("until_$noteId")

    override suspend fun attempts(noteId: String): Int =
        dataStore.data.first()[attemptsKey(noteId)] ?: 0

    override suspend fun lockoutUntil(noteId: String): Long =
        dataStore.data.first()[untilKey(noteId)] ?: 0L

    override suspend fun save(noteId: String, attempts: Int, lockoutUntil: Long) {
        dataStore.edit { prefs ->
            prefs[attemptsKey(noteId)] = attempts
            if (lockoutUntil > 0L) {
                prefs[untilKey(noteId)] = lockoutUntil
            } else {
                prefs.remove(untilKey(noteId))
            }
        }
    }

    override suspend fun clear(noteId: String) {
        dataStore.edit { prefs ->
            prefs.remove(attemptsKey(noteId))
            prefs.remove(untilKey(noteId))
        }
    }
}
