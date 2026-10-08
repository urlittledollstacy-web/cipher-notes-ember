package dev.cipher.notes.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * Remaining wait in milliseconds, recomputed every second while a lockout runs
 * so callers can react to it reaching zero.
 */
@Composable
fun rememberLockoutRemainingMs(lockoutUntil: Long): Long {
    var now by remember(lockoutUntil) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lockoutUntil) {
        while (System.currentTimeMillis() < lockoutUntil) {
            now = System.currentTimeMillis()
            delay(1000)
        }
        now = System.currentTimeMillis()
    }
    return (lockoutUntil - now).coerceAtLeast(0L)
}

/**
 * Live "try again in Ns" line for the note-unlock lockout.
 *
 * The wait is carried as a deadline rather than a pre-formatted string so the
 * number ticks down on its own. A frozen string forced the user to retry just
 * to see how long was left.
 */
@Composable
fun UnlockCountdown(lockoutUntil: Long, modifier: Modifier = Modifier) {
    val remainingMs = rememberLockoutRemainingMs(lockoutUntil)
    val secondsLeft = (remainingMs + 999L) / 1000L
    if (secondsLeft > 0L) {
        Text(
            text = "Try again in ${secondsLeft}s",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = modifier
        )
    }
}

