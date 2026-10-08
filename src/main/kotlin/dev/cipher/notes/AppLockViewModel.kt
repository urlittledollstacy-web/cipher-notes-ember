package dev.cipher.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Remembers that the app lock has been satisfied for this process.
 *
 * Living in a ViewModel rather than `remember` means a rotation does not drop
 * the user back to the lock screen. It is deliberately not persisted: a cold
 * start should lock again.
 */
@HiltViewModel
class AppLockViewModel @Inject constructor() : ViewModel() {

    var isAuthenticated by mutableStateOf(false)
        private set

    fun markAuthenticated() {
        isAuthenticated = true
    }
}
