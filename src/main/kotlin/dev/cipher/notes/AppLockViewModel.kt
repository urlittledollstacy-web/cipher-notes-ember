package dev.cipher.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.cipher.notes.ui.theme.ThemeMode
import javax.inject.Inject

/**
 * Process-scoped state for the app lock and the startup theme.
 *
 * Both exist to remove the first-frame guess on rotation. The theme values are
 * DataStore reads and the note list is a Room read; on the first frame after a
 * device rotation none of them have emitted yet, so whatever `initial` value
 * the caller passed would be painted. That produced a flash of the dynamic
 * colour scheme (the `initial = true` guess when the user had picked a custom
 * theme) and, for the lock, a frame where the notes were composed before the
 * real lock state arrived.
 *
 * Caching the resolved values here means the recreated activity paints
 * correctly on its first frame instead of guessing.
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Set once the lock state has been read from storage; null means unknown. */
    var isAppLockEnabled by mutableStateOf<Boolean?>(null)
        private set

    /** Set once the theme has been read; null means unknown. */
    var dynamicColors by mutableStateOf<Boolean?>(null)
        private set

    var themeMode by mutableStateOf<ThemeMode?>(null)
        private set

    /**
     * Whether the app lock has been satisfied.
     *
     * Snapshot state so Compose recomposes when it flips, and mirrored into
     * [SavedStateHandle] so ProcessDeath recovery keeps it. Reading the handle
     * in a plain getter would not be observable and the lock screen would never
     * advance. Deliberately not persisted: a cold start must lock again.
     */
    var isAuthenticated by mutableStateOf(savedStateHandle.get<Boolean>(AUTHENTICATED_KEY) ?: false)
        private set

    fun cacheLockEnabled(enabled: Boolean) {
        isAppLockEnabled = enabled
    }

    fun cacheTheme(dynamic: Boolean, mode: ThemeMode) {
        dynamicColors = dynamic
        themeMode = mode
    }

    fun markAuthenticated() {
        isAuthenticated = true
        savedStateHandle[AUTHENTICATED_KEY] = true
    }

    private companion object {
        const val AUTHENTICATED_KEY = "app_lock_authenticated"
    }
}
