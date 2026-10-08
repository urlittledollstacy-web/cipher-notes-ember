package dev.cipher.notes.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteRepository
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.crypto.PinHasher
import dev.cipher.notes.crypto.PinThrottle
import dev.cipher.notes.ui.theme.ThemeMode
import dev.cipher.notes.widget.NotesWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: NoteRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        // Shared with the widget, which needs the theme but has no Hilt graph.
        val DYNAMIC_COLORS_KEY = booleanPreferencesKey("use_dynamic_colors")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val APP_LOCK_KEY = booleanPreferencesKey("app_lock_enabled")
        private val APP_PIN_KEY = stringPreferencesKey("app_pin")
        // Kept in DataStore, not in the composable: the app lock must not be
        // reset by rotating the device or killing the app.
        private val APP_PIN_FAILED_KEY = intPreferencesKey("app_pin_failed_attempts")
        private val APP_PIN_LOCKOUT_KEY = longPreferencesKey("app_pin_lockout_until")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val WIDGET_CONTENT_VISIBLE_KEY = booleanPreferencesKey("widget_content_visible")
        private val SEAL_NEW_NOTES_KEY = booleanPreferencesKey("seal_new_notes_by_default")

        // The app lock stands in front of every note, so it throttles like the
        // note unlock rather than allowing unlimited 4-digit guesses.
        // See PinThrottle for the lockout curve.

        val SELECTED_NOTE_IDS_KEY = stringSetPreferencesKey("selected_note_ids")


        private val HEADER_MAGIC = "CIPHER_N".toByteArray(Charsets.UTF_8)
    }

    val allNotes: Flow<List<Note>> = repo.getAllNotes()

    val pinnedNoteIds: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SELECTED_NOTE_IDS_KEY] ?: emptySet()
    }

    fun updateSelectedWidgetNotes(selectedIds: Set<String>) {
        viewModelScope.launch {
            val limitedIds = selectedIds.take(4).toSet()

            dataStore.edit { preferences ->
                preferences[SELECTED_NOTE_IDS_KEY] = limitedIds
            }

            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(NotesWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[SELECTED_NOTE_IDS_KEY] = limitedIds
                    }
                }
                NotesWidget().update(context, glanceId)
            }
        }
    }

    val useDynamicColors: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLORS_KEY] ?: true
        }

    // Existing installs only ever had the dynamic-colors boolean. Read the new
    // key first, then default: dynamic off meant the user wanted Ember, dynamic
    // on (the default they never touched) meant Light.
    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { preferences ->
            val stored = preferences[THEME_MODE_KEY]
            if (stored != null) {
                ThemeMode.fromKey(stored)
            } else {
                when (preferences[DYNAMIC_COLORS_KEY]) {
                    false -> ThemeMode.EMBER
                    true -> ThemeMode.LIGHT
                    null -> ThemeMode.DEFAULT
                }
            }
        }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = mode.key
            }
            refreshWidgetTheme(mode)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DYNAMIC_COLORS_KEY] = enabled
            }
            refreshWidgetTheme(themeMode.first())
        }
    }

    /**
     * The widget lives in its own Glance state, so it cannot observe DataStore.
     * Mirror the resolved theme across and re-render.
     */
    private suspend fun refreshWidgetTheme(mode: ThemeMode) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(NotesWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[NotesWidget.THEME_MODE_KEY] = mode.key
                }
            }
            NotesWidget().update(context, glanceId)
        }
    }

    val isAppLockEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[APP_LOCK_KEY] ?: false
        }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[APP_LOCK_KEY] = enabled
            }
        }
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: true
        }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BIOMETRIC_ENABLED_KEY] = enabled
            }
        }
    }

    // Sealing on creation is on by default so a note is never written to the
    // plaintext database unless the user explicitly declines.
    val sealNewNotesByDefault: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SEAL_NEW_NOTES_KEY] ?: true
        }

    fun setSealNewNotesByDefault(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SEAL_NEW_NOTES_KEY] = enabled
            }
        }
    }

    val isWidgetContentVisible: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[WIDGET_CONTENT_VISIBLE_KEY] ?: false
        }

    fun setWidgetContentVisible(visible: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[WIDGET_CONTENT_VISIBLE_KEY] = visible
            }

            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(NotesWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WIDGET_CONTENT_VISIBLE_KEY] = visible
                    }
                }
                NotesWidget().update(context, glanceId)
            }
        }
    }

    /**
     * Whether an app-lock PIN is set. The raw PIN is never exposed: it is only
     * ever read inside [verifyAppPin] and [setAppPin].
     */
    val isAppPinSet: Flow<Boolean> = dataStore.data
        .map { it[APP_PIN_KEY] != null }

    /** Verification outcome, so the lock screen can show a countdown without seeing the PIN. */
    sealed interface PinResult {
        data object Success : PinResult
        data object Wrong : PinResult
        data class Locked(val remainingMs: Long) : PinResult
    }

    /**
     * Verifies a PIN against the stored hash, migrating a plaintext PIN on first use.
     *
     * The failed-attempt count and lockout deadline live in DataStore, so they
     * survive both rotation and the app being killed. Without that, the throttle
     * was bypassable simply by rotating the device.
     */
    suspend fun verifyAppPin(pin: String): PinResult = withContext(Dispatchers.Default) {
        val prefs = dataStore.data.first()
        val stored = prefs[APP_PIN_KEY] ?: return@withContext PinResult.Wrong

        val remaining = (prefs[APP_PIN_LOCKOUT_KEY] ?: 0L) - System.currentTimeMillis()
        if (remaining > 0L) return@withContext PinResult.Locked(remaining)

        val correct = PinHasher.verify(pin, stored) ||
            (PinHasher.isLegacyPlaintext(stored) && stored == pin)

        if (correct) {
            if (PinHasher.isLegacyPlaintext(stored)) {
                // Existing install: the PIN is stored in the clear. Hash it now so
                // the same PIN keeps working and the plaintext copy goes away.
                dataStore.edit { it[APP_PIN_KEY] = PinHasher.hash(pin) }
            }
            dataStore.edit {
                it.remove(APP_PIN_FAILED_KEY)
                it.remove(APP_PIN_LOCKOUT_KEY)
            }
            PinResult.Success
        } else {
            // The count has to be committed before returning, otherwise a
            // rotation mid-check would lose the attempt.
            var lockedUntil = 0L
            dataStore.edit { preferences ->
                val attempts = (preferences[APP_PIN_FAILED_KEY] ?: 0) + 1
                preferences[APP_PIN_FAILED_KEY] = attempts
                val backoff = PinThrottle.lockoutMs(attempts)
                if (backoff > 0L) {
                    lockedUntil = System.currentTimeMillis() + backoff
                    preferences[APP_PIN_LOCKOUT_KEY] = lockedUntil
                }
            }
            if (lockedUntil > 0L) {
                PinResult.Locked((lockedUntil - System.currentTimeMillis()).coerceAtLeast(0L))
            } else {
                PinResult.Wrong
            }
        }
    }

    /**
     * Remaining lockout in milliseconds, or 0 if the lock is not throttled.
     *
     * The lock screen needs this on composition: the deadline is persisted, but
     * the composable holding the countdown is rebuilt on rotation, so it has to
     * ask storage again or the lockout appears to vanish.
     */
    suspend fun activeLockoutMs(): Long = withContext(Dispatchers.Default) {
        val until = dataStore.data.first()[APP_PIN_LOCKOUT_KEY] ?: 0L
        (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun setAppPin(pin: String?) {
        viewModelScope.launch {
            val hashed = pin?.let { withContext(Dispatchers.Default) { PinHasher.hash(it) } }
            dataStore.edit { preferences ->
                if (hashed == null) {
                    preferences.remove(APP_PIN_KEY)
                } else {
                    preferences[APP_PIN_KEY] = hashed
                }
                // A changed or removed PIN starts from a clean slate; a lockout
                // against the old PIN should not carry over.
                preferences.remove(APP_PIN_FAILED_KEY)
                preferences.remove(APP_PIN_LOCKOUT_KEY)
            }
        }
    }

    fun nuclearWipe() {
        viewModelScope.launch {
            repo.deleteAllNotes()
            dataStore.edit { preferences ->
                preferences.clear()
            }
            updateSelectedWidgetNotes(emptySet())
        }
    }


    fun exportBackup(context: Context, uri: Uri, password: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val notes = repo.getAllNotes().first()
                val jsonArray = JSONArray()

                notes.forEach { note ->
                    val jsonNote = JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("content", note.content)
                        put("itemsJson", note.itemsJson)
                        put("createdAt", note.createdAt)
                        put("modifiedAt", note.modifiedAt)
                        put("encrypted", note.encrypted)
                        put("ciphertext", note.ciphertext ?: JSONObject.NULL)
                        put("type", note.type.name)
                    }
                    jsonArray.put(jsonNote)
                }

                val backupObject = JSONObject().apply {
                    put("version", 1)
                    put("notes", jsonArray)
                }

                val rawBytes = backupObject.toString(2).toByteArray(Charsets.UTF_8)


                val bytesToWrite = if (!password.isNullOrEmpty()) {
                    encryptData(rawBytes, password)
                } else {
                    rawBytes
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytesToWrite)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    fun importBackup(
        context: Context,
        uri: Uri,
        password: String? = null,
        onPasswordRequired: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("Unable to read file")

                val jsonString = if (isCipherFile(bytes)) {
                    if (password.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) { onPasswordRequired() }
                        return@launch
                    }
                    val decryptedBytes = decryptData(bytes, password)
                    String(decryptedBytes, Charsets.UTF_8)
                } else {
                    String(bytes, Charsets.UTF_8)
                }

                val backupObject = JSONObject(jsonString)
                val jsonArray = backupObject.optJSONArray("notes") ?: JSONArray()

                var importedCount = 0
                for (i in 0 until jsonArray.length()) {
                    val jsonNote = jsonArray.getJSONObject(i)
                    val noteType = try {
                        NoteType.valueOf(jsonNote.optString("type", NoteType.entries.first().name))
                    } catch (e: Exception) {
                        NoteType.entries.first()
                    }

                    val ciphertextValue = if (jsonNote.isNull("ciphertext")) null else jsonNote.optString("ciphertext", null)

                    val note = Note(
                        id = jsonNote.optString("id", java.util.UUID.randomUUID().toString()),
                        title = jsonNote.optString("title", ""),
                        content = jsonNote.optString("content", ""),
                        itemsJson = jsonNote.optString("itemsJson", "[]"),
                        createdAt = jsonNote.optLong("createdAt", System.currentTimeMillis()),
                        modifiedAt = jsonNote.optLong("modifiedAt", System.currentTimeMillis()),
                        encrypted = jsonNote.optBoolean("encrypted", false),
                        ciphertext = ciphertextValue,
                        type = noteType
                    )

                    repo.insertOrUpdateNote(note)
                    importedCount++
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Imported $importedCount notes!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (password != null) "Incorrect password or corrupted file!" else "Import failed: Invalid JSON file",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun isCipherFile(bytes: ByteArray): Boolean {
        if (bytes.size < HEADER_MAGIC.size) return false
        return bytes.copyOfRange(0, HEADER_MAGIC.size).contentEquals(HEADER_MAGIC)
    }

    private fun encryptData(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val encryptedData = cipher.doFinal(data)


        return HEADER_MAGIC + salt + iv + encryptedData
    }

    private fun decryptData(data: ByteArray, password: String): ByteArray {
        var offset = HEADER_MAGIC.size
        val salt = data.copyOfRange(offset, offset + 16)
        offset += 16
        val iv = data.copyOfRange(offset, offset + 12)
        offset += 12
        val encryptedData = data.copyOfRange(offset, data.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedData)
    }
}