package dev.cipher.notes

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.cipher.notes.crypto.BiometricPromptManager
import dev.cipher.notes.ui.CipherMainApp
import dev.cipher.notes.ui.screens.SettingsViewModel
import dev.cipher.notes.ui.theme.CipherTheme
import dev.cipher.notes.ui.theme.ThemeMode
import kotlinx.coroutines.launch

// The app lock stands in front of every note, so it throttles like the note
// unlock rather than allowing unlimited 4-digit guesses.
private const val MAX_PIN_ATTEMPTS = 5
private const val BASE_PIN_LOCKOUT_MS = 5_000L

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        val sharedText = extractSharedText(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val useDynamicColors by settingsViewModel.useDynamicColors.collectAsState(initial = true)
            val themeMode by settingsViewModel.themeMode.collectAsState(initial = ThemeMode.DEFAULT)
            val isAppLockEnabled by settingsViewModel.isAppLockEnabled.collectAsState(initial = false)
            val isBiometricEnabledState by settingsViewModel.isBiometricEnabled.collectAsState(initial = null)

            var isAuthenticated by remember { mutableStateOf(false) }

            CipherTheme(
                themeMode = themeMode,
                dynamicColors = useDynamicColors
            ) {
                ThemedSystemBars()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAppLockEnabled && !isAuthenticated) {
                        val biometricEnabled = (isBiometricEnabledState == true) &&
                                BiometricPromptManager.canAuthenticate(this@MainActivity)

                        LockScreen(
                            verifyPin = settingsViewModel::verifyAppPin,
                            biometricEnabled = biometricEnabled,
                            onUnlockRequest = {
                                if (biometricEnabled) {
                                    showBiometricPrompt(
                                        onSuccess = { isAuthenticated = true },
                                        onError = { /* or type PIN */ }
                                    )
                                }
                            },
                            onAuthenticated = { isAuthenticated = true }
                        )
                    } else {
                        CipherMainApp(sharedText = sharedText)
                    }
                }
            }
        }
    }

    /**
     * Keeps the status/navigation bar icon contrast in sync with the resolved
     * theme. Without this a light theme inherits white-on-white system icons.
     */
    @Composable
    private fun ThemedSystemBars() {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val barColor = MaterialTheme.colorScheme.background
        val barStyle = SystemBarStyle.auto(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        ) { isDark }

        LaunchedEffect(barColor, isDark) {
            enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            window.setNavigationBarColor(barColor.toArgb())
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit, onError: () -> Unit) {
        val promptManager = BiometricPromptManager(this)
        promptManager.showBiometricPrompt(
            title = "CipherNotes Locked",
            subtitle = "Authenticate to unlock application",
            negativeButtonText = "Use App PIN",
            onSuccess = onSuccess,
            onError = { onError() }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("text/") != true) return null

        return runCatching {
            val directString = intent.getStringExtra(Intent.EXTRA_TEXT)
            val charSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            val htmlText = intent.getStringExtra(Intent.EXTRA_HTML_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_HTML_TEXT)?.toString()

            val resultText = directString ?: charSequence?.toString() ?: htmlText
            resultText?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

@Composable
fun LockScreen(
    verifyPin: suspend (String) -> Boolean,
    biometricEnabled: Boolean,
    onUnlockRequest: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var failedAttempts by remember { mutableStateOf(0) }
    var lockoutUntil by remember { mutableStateOf(0L) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    val lockedForMs = (lockoutUntil - now).coerceAtLeast(0L)

    LaunchedEffect(lockoutUntil) {
        while (System.currentTimeMillis() < lockoutUntil) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
        now = System.currentTimeMillis()
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            onUnlockRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = "Biometric Lock",
                modifier = Modifier
                    .size(64.dp)
                    .clickable(enabled = biometricEnabled) { onUnlockRequest() },
                tint = if (biometricEnabled) primaryColor else primaryColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CipherNotes Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape,
                        color = if (isFilled) primaryColor else primaryColor.copy(alpha = 0.2f),
                        border = if (!isFilled) BorderStroke(1.dp, primaryColor) else null
                    ) {}
                }
            }
        }

        Column(
            modifier = Modifier.width(280.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (lockedForMs > 0L) {
                Text(
                    text = "Too many attempts. Try again in ${lockedForMs / 1000 + 1}s",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "C")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        if (digit.isEmpty()) {
                            Spacer(modifier = Modifier.size(64.dp))
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    if (digit == "C") {
                                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                    } else if (enteredPin.length < 4 && lockedForMs <= 0L) {
                                        enteredPin += digit
                                        if (enteredPin.length == 4) {
                                            val attempt = enteredPin
                                            enteredPin = ""
                                            scope.launch {
                                                if (verifyPin(attempt)) {
                                                    failedAttempts = 0
                                                    onAuthenticated()
                                                } else {
                                                    failedAttempts++
                                                    if (failedAttempts >= MAX_PIN_ATTEMPTS) {
                                                        // A 4-digit PIN is only 10k
                                                        // combinations; this is what
                                                        // stops them being tried in bulk.
                                                        lockoutUntil = System.currentTimeMillis() +
                                                            BASE_PIN_LOCKOUT_MS * (1L shl (failedAttempts - MAX_PIN_ATTEMPTS).coerceAtMost(6))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(digit, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }

        if (biometricEnabled) {
            TextButton(onClick = onUnlockRequest) {
                Text("Use Biometrics", color = primaryColor, fontWeight = FontWeight.Medium)
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}