package dev.cipher.notes.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.cipher.notes.crypto.CryptoManager
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteRepository
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.widget.NotesWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val filterBy: String = "all",
    val sortBy: String = "modified",
    val pinnedIds: Set<String> = emptySet(),
    val bioNoteIds: Set<String> = emptySet(),
    val pendingDelete: Note? = null
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repo: NoteRepository,
    private val crypto: CryptoManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var pendingDeleteJob: Job? = null

    private val prefs: SharedPreferences = context.getSharedPreferences("pinned_notes_prefs", Context.MODE_PRIVATE)

    private val _pendingDeleteId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _filterBy = MutableStateFlow("all")
    private val _sortBy = MutableStateFlow("modified")
    private val _pinnedIds = MutableStateFlow(getPinnedIdsFromPrefs())

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private data class ListSource(
        val allNotes: List<Note>,
        val query: String,
        val filter: String,
        val sort: String,
        val pinnedIds: Set<String>
    )

    init {
        viewModelScope.launch {
            combine(
                repo.getAllNotes(),
                _searchQuery,
                _filterBy,
                _sortBy,
                _pinnedIds
            ) { allNotes, query, filter, sort, pinnedIds ->
                ListSource(allNotes, query, filter, sort, pinnedIds)
            }.combine(_pendingDeleteId) { source, pendingId ->
                // A note awaiting undo stays in the DB, so filter it out of the
                // visible list here rather than deleting it eagerly.
                val visibleNotes =
                    applyFiltersAndSort(source.allNotes, source.query, source.filter, source.sort, source.pinnedIds)
                        .filterNot { it.id == pendingId }
                // Biometric state lives in EncryptedSharedPreferences, which is
                // not reactive. Reading it decrypts, so do it off the main
                // thread and recompute per emission to avoid a stale badge.
                val bioNoteIds = visibleNotes
                    .filter { it.encrypted }
                    .map { it.id }
                    .filter { id ->
                        withContext(Dispatchers.IO) { crypto.hasBiometricPassword(id) }
                    }
                    .toSet()
                ListUiState(
                    notes = visibleNotes,
                    searchQuery = source.query,
                    filterBy = source.filter,
                    sortBy = source.sort,
                    pinnedIds = source.pinnedIds,
                    bioNoteIds = bioNoteIds,
                    pendingDelete = source.allNotes.firstOrNull { it.id == pendingId }
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun togglePin(noteId: String) {
        val currentPinned = _pinnedIds.value.toMutableSet()
        if (currentPinned.contains(noteId)) {
            currentPinned.remove(noteId)
        } else {
            currentPinned.add(noteId)
        }
        prefs.edit().putStringSet("pinned_ids", currentPinned).apply()
        _pinnedIds.value = currentPinned
    }

    private fun getPinnedIdsFromPrefs(): Set<String> {
        return prefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilter(filter: String) { _filterBy.value = filter }
    fun setSortBy(sort: String) { _sortBy.value = sort }

    /**
     * Hides [id] from the list and starts the undo window. The row is left in
     * the database, so [undoDelete] is lossless: no ciphertext is touched and
     * the biometric passphrase is never removed. Only when the window closes
     * without an undo does the note actually go away.
     */
    fun requestDelete(id: String) {
        pendingDeleteJob?.cancel()
        _pendingDeleteId.value = id
        pendingDeleteJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            commitDelete(id)
        }
    }

    fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _pendingDeleteId.value = null
    }

    private suspend fun commitDelete(id: String) {
        val note = repo.getNoteById(id)
        if (note?.encrypted == true) {
            crypto.removeBiometricPassword(id)
        }
        repo.deleteNote(id)
        if (_pinnedIds.value.contains(id)) {
            togglePin(id)
        }
        NotesWidget().updateAll(context)
        _pendingDeleteId.value = null
        pendingDeleteJob = null
    }

    companion object {
        const val UNDO_WINDOW_MS = 7_000L
    }

    fun createNote(type: NoteType, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newNote = repo.createNote(type)


            delay(100)



            onCreated(newNote.id)
        }
    }

    private fun applyFiltersAndSort(
        notes: List<Note>,
        query: String,
        filter: String,
        sort: String,
        pinnedIds: Set<String>
    ): List<Note> {
        var filtered = notes

        filtered = when (filter) {
            "note"      -> filtered.filter { it.type == NoteType.TEXT }
            "todo"      -> filtered.filter { it.type == NoteType.TODO }
            "encrypted" -> filtered.filter { it.encrypted }
            else        -> filtered
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        (!it.encrypted && it.content.contains(query, ignoreCase = true))
            }
        }

        filtered = when (sort) {
            "created" -> filtered.sortedByDescending { it.createdAt }
            "title"   -> filtered.sortedBy { it.title }
            else      -> filtered.sortedByDescending { it.modifiedAt }
        }
        return filtered.sortedByDescending { pinnedIds.contains(it.id) }
    }
}