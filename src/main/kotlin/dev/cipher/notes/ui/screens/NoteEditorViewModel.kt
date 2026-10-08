package dev.cipher.notes.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.text.input.TextFieldValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cipher.notes.crypto.CryptoManager
import dev.cipher.notes.crypto.LockoutController
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteRepository
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.data.TodoItem
import dev.cipher.notes.utils.JsonUtils
import dev.cipher.notes.widget.NotesWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

data class EditorUiState(
    val note: Note? = null,
    val title: String = "",
    val content: TextFieldValue = TextFieldValue(""),
    val items: List<TodoItem> = emptyList(),
    val encrypted: Boolean = false,
    val isLocked: Boolean = false,
    val hasBiometric: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val promptSeal: Boolean = false,
    /** Wall-clock instant the unlock lockout ends, for a live countdown. 0 when not locked out. */
    val lockoutUntil: Long = 0L
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repo: NoteRepository,
    private val crypto: CryptoManager,
    private val lockout: LockoutController,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private companion object {
        /**
         * Quiet period before a typed change is written. Long enough that a
         * burst of keystrokes collapses into one write, short enough that the
         * note is on disk almost immediately after typing stops.
         */
        const val SAVE_DEBOUNCE_MS = 400L
    }

    private val noteId: String? = savedStateHandle["noteId"]
    private val promptSealOnOpen: Boolean =
        savedStateHandle.get<String>("promptSeal")?.toBoolean() ?: false
    private var currentUserPassword: String? = null

    /**
     * Serializes every write. Saves are launched concurrently (one per
     * keystroke), so without this an earlier, shorter snapshot can land after a
     * later one and win, leaving the note on an old title or content.
     */
    private val writeMutex = Mutex()
    private var debounceJob: Job? = null
    private var deleted = false

    /**
     * A scope that outlives [viewModelScope] so the final flush on exit is not
     * cancelled when the screen closes. Writes here are also non-cancellable:
     * leaving the editor is a normal action, but a cancelled write is how a
     * seal or a last keystroke silently vanished.
     */
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        if (noteId != null) {
            viewModelScope.launch {
                val note = repo.getNoteById(noteId) ?: return@launch

                val rawSharedText = savedStateHandle.get<String>("sharedText")

                val incomingSharedText = rawSharedText?.let {
                    val decoded = android.net.Uri.decode(it)
                    sanitizeSharedText(decoded)
                }

                val finalContentText = incomingSharedText ?: note.content

                _uiState.value = EditorUiState(
                    note = note,
                    title = note.title,
                    content = TextFieldValue(text = finalContentText),
                    items = if (note.type == NoteType.TODO) JsonUtils.jsonToTodoItems(note.itemsJson) else emptyList(),
                    encrypted = note.encrypted,
                    isLocked = note.encrypted,
                    hasBiometric = crypto.hasBiometricPassword(note.id),
                    promptSeal = promptSealOnOpen && !note.encrypted
                )

                if (incomingSharedText != null) {
                    save()
                }
            }
        }
    }

    private var _sharedTextProcessed = false

    fun shouldProcessSharedText(text: String?): Boolean {
        if (text == null) return false
        if (_sharedTextProcessed) return false
        return true
    }

    fun markSharedTextAsProcessed() {
        _sharedTextProcessed = true
    }

    fun setTitle(t: String) {
        _uiState.update { it.copy(title = t) }
        scheduleSave()
    }

    fun setContent(newValue: TextFieldValue) {
        _uiState.update { it.copy(content = newValue) }
        scheduleSave()
    }

    fun addTodoItem(text: String = "") {
        val currentItems = _uiState.value.items.toMutableList()
        currentItems.add(JsonUtils.newTodoItem(text))
        _uiState.update { it.copy(items = currentItems) }
        save()
    }

    fun updateTodoItem(id: String, text: String? = null, done: Boolean? = null) {
        val currentItems = _uiState.value.items.toMutableList()
        val idx = currentItems.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val item = currentItems[idx]
            currentItems[idx] = item.copy(text = text ?: item.text, done = done ?: item.done)
            _uiState.update { it.copy(items = currentItems) }
            save()
        }
    }

    fun deleteTodoItem(id: String) {
        val currentItems = _uiState.value.items.filter { it.id != id }
        _uiState.update { it.copy(items = currentItems) }
        save()
    }

    fun save() {
        viewModelScope.launch { writeLatest() }
    }

    /**
     * Saves typed changes after a short quiet period.
     *
     * Typing fires this once per keystroke. Restarting the timer each time means
     * one write lands once typing stops, instead of one per character racing the
     * others to the database.
     */
    private fun scheduleSave() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            writeLatest()
        }
    }

    /**
     * Writes the current state, in order.
     *
     * Every write takes [writeMutex], so concurrent saves cannot land out of
     * order and leave an older snapshot as the final word.
     */
    private suspend fun writeLatest() {
        writeMutex.withLock { writeCurrentState() }
    }

    /**
     * Flushes any pending typed change on exit, without letting the closing
     * screen cancel it. Called from [onCleared]; the work runs on
     * [cleanupScope] because [viewModelScope] is already cancelled by then.
     */
    fun flushPendingSaves() {
        debounceJob?.cancel()
        cleanupScope.launch {
            withContext(NonCancellable) { writeLatest() }
        }
    }

    override fun onCleared() {
        flushPendingSaves()
        super.onCleared()
    }

    private suspend fun writeCurrentState() {
        val state = _uiState.value
        if (state.isLocked || deleted) return

        val note = state.note ?: Note(
            id = noteId ?: java.util.UUID.randomUUID().toString(),
            title = state.title.trim(),
            content = state.content.text,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )

        try {
            val updatedCiphertext = if (state.encrypted && currentUserPassword != null) {
                val payload = JSONObject().apply {
                    put("title", state.title)
                    if (note.type == NoteType.TODO) {
                        put("items", JsonUtils.todoItemsToJson(state.items))
                    } else {
                        put("content", state.content.text)
                    }
                }

                withContext(Dispatchers.Default) {
                    crypto.encrypt(payload.toString(), currentUserPassword!!)
                }
            } else {
                note.ciphertext
            }

            val updated = note.copy(
                title = state.title.trim(),
                content = if (!state.encrypted && note.type == NoteType.TEXT) state.content.text else "",
                itemsJson = if (!state.encrypted && note.type == NoteType.TODO) JsonUtils.todoItemsToJson(state.items) else "[]",
                ciphertext = updatedCiphertext,
                modifiedAt = System.currentTimeMillis()
            )

            repo.saveNote(updated)
            _uiState.update { it.copy(note = updated) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Save failed: ${e.message}") }
        }
    }

    /**
     * Publishes a sealed note.
     *
     * The encryption and the write run on [cleanupScope] and are
     * non-cancellable, because leaving the editor used to cancel them and leave
     * the note silently unsealed while the user believed it was locked.
     */
    fun performEncrypt(password: String, enableBiometric: Boolean = false) {
        cleanupScope.launch {
            withContext(NonCancellable) {
                val state = _uiState.value
                val note = state.note ?: return@withContext

                // Serialize with ordinary saves and cancel any pending one, so a
                // later autosave cannot overwrite this sealed snapshot with the
                // still-unencrypted state.
                debounceJob?.cancel()
                writeMutex.withLock {
                    try {
                        val payload = JSONObject().apply {
                            put("title", state.title)
                            if (note.type == NoteType.TEXT) {
                                put("content", state.content.text)
                            } else {
                                put("items", JsonUtils.todoItemsToJson(state.items))
                            }
                        }

                        val cipher = withContext(Dispatchers.Default) {
                            crypto.encrypt(payload.toString(), password)
                        }

                        currentUserPassword = password

                        if (enableBiometric) {
                            crypto.savePasswordForBiometric(note.id, password)
                        } else {
                            crypto.removeBiometricPassword(note.id)
                        }

                        val encryptedNote = note.copy(
                            ciphertext = cipher,
                            encrypted = true,
                            content = "",
                            itemsJson = "[]",
                            modifiedAt = System.currentTimeMillis()
                        )
                        repo.saveNote(encryptedNote)

                        _uiState.update { it.copy(
                            note = encryptedNote,
                            encrypted = true,
                            isLocked = true,
                            hasBiometric = enableBiometric,
                            error = null
                        ) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = "Encryption failed") }
                    }
                }
            }
        }
    }

    fun unlock(password: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val note = state.note ?: return@launch
            val id = note.id

            // Consulted fresh from storage each time: the wait outlives this
            // view model, so a lockout set before leaving the note still holds.
            val remainingMs = lockout.remainingLockoutMs(id, System.currentTimeMillis())
            if (remainingMs > 0) {
                _uiState.update {
                    it.copy(
                        error = "Too many attempts",
                        isLocked = true,
                        lockoutUntil = System.currentTimeMillis() + remainingMs
                    )
                }
                return@launch
            }

            try {
                val ciphertext = note.ciphertext ?: return@launch
                val decryptedJson = withContext(Dispatchers.Default) {
                    crypto.decrypt(ciphertext, password)
                }

                val payload = JSONObject(decryptedJson)
                currentUserPassword = password
                lockout.recordSuccess(id)

                if (state.hasBiometric || crypto.hasBiometricPassword(id)) {
                    crypto.savePasswordForBiometric(id, password)
                }

                _uiState.update { it.copy(
                    isLocked = false,
                    title = payload.optString("title", note.title),
                    content = TextFieldValue(text = payload.optString("content", "")),
                    items = if (note.type == NoteType.TODO) {
                        JsonUtils.jsonToTodoItems(payload.optString("items", "[]"))
                    } else emptyList(),
                    hasBiometric = crypto.hasBiometricPassword(id),
                    error = null,
                    lockoutUntil = 0L
                ) }
            } catch (e: Exception) {
                val backoff = lockout.recordFailure(id, System.currentTimeMillis())
                _uiState.update { it.copy(
                    error = "Wrong password",
                    isLocked = true,
                    lockoutUntil = if (backoff > 0L) System.currentTimeMillis() + backoff else 0L
                ) }
            }
        }
    }

    fun dismissSealPrompt() {
        _uiState.update { it.copy(promptSeal = false) }
    }

    /**
     * Removes a note's seal, storing its contents in the clear.
     *
     * Only reachable once the note is unlocked, so the plaintext is already in
     * state and this is a save with the seal dropped rather than a decrypt.
     */
    fun unseal() {
        cleanupScope.launch {
            withContext(NonCancellable) {
                val state = _uiState.value
                val note = state.note ?: return@withContext
                if (state.isLocked) return@withContext
                debounceJob?.cancel()
                writeMutex.withLock {
                    try {
                        val unsealed = note.copy(
                            title = state.title.trim(),
                            encrypted = false,
                            ciphertext = null,
                            content = if (note.type == NoteType.TEXT) state.content.text else "",
                            itemsJson = if (note.type == NoteType.TODO) JsonUtils.todoItemsToJson(state.items) else "[]",
                            modifiedAt = System.currentTimeMillis()
                        )
                        repo.saveNote(unsealed)
                        // Nothing should keep a biometric unlock pointing at a note
                        // that is now plaintext.
                        crypto.removeBiometricPassword(note.id)
                        // Drop the cached password so the next save cannot re-seal.
                        currentUserPassword = null
                        _uiState.update { it.copy(
                            note = unsealed,
                            encrypted = false,
                            isLocked = false,
                            hasBiometric = false,
                            error = null
                        ) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = "Unseal failed") }
                    }
                }
            }
        }
    }

    fun unlockWithBiometric() {
        val note = _uiState.value.note ?: return
        val savedPassword = crypto.getPasswordFromBiometric(note.id)
        if (savedPassword != null) {
            unlock(savedPassword)
        } else {
            _uiState.update { it.copy(error = "Biometric data missing or invalid") }
        }
    }

    fun exportNote(context: Context) {
        val state = _uiState.value
        if (state.isLocked) {
            Toast.makeText(context, "Unlock note first to export", Toast.LENGTH_SHORT).show()
            return
        }
        val textToExport = "Title: ${state.title}\n\n${state.content.text}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, state.title)
            putExtra(Intent.EXTRA_TEXT, textToExport)
        }
        context.startActivity(Intent.createChooser(intent, "Export Note"))
    }

    fun delete() {
        cleanupScope.launch {
            withContext(NonCancellable) {
                // Mark deleted before the write lock so the onCleared flush
                // cannot re-insert the note we are about to remove.
                deleted = true
                debounceJob?.cancel()
                writeMutex.withLock {
                    val note = _uiState.value.note ?: return@withLock
                    crypto.removeBiometricPassword(note.id)
                    repo.deleteNote(note.id)
                }
            }
        }
    }

    private fun sanitizeSharedText(text: String): String {
        var raw = text

        if (raw.contains("#:~:text=")) {
            raw = raw.replace(Regex("#:~:text=.*"), "")
        }

        val httpUrlRegex = Regex("https?://[^\\s]+")
        val matchResult = httpUrlRegex.find(raw)

        val extractedUrl = matchResult?.value
        var cleanText = if (extractedUrl != null) raw.replace(extractedUrl, "") else raw

        cleanText = cleanText
            .replace(Regex("\\[\\d+\\]"), "")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" +([.,!?:;])"), "$1")
            .replace(",.", ".")
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .trim()

        return if (!extractedUrl.isNullOrBlank()) {
            if (cleanText.isNotEmpty()) {
                "$cleanText\n\nSource: $extractedUrl"
            } else {
                extractedUrl
            }
        } else {
            cleanText
        }
    }
}