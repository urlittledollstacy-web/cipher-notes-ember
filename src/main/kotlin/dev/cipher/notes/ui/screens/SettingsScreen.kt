package dev.cipher.notes.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.notes.crypto.BiometricPromptManager
import dev.cipher.notes.ui.theme.ThemeMode
import dev.cipher.notes.ui.theme.cipherSwitchColors
import dev.cipher.notes.ui.theme.themeSwatches

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNuclearWipeComplete: () -> Unit = onBack,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showRemovePinConfirm by remember { mutableStateOf(false) }
    var showWidgetNotesPicker by remember { mutableStateOf(false) }
    var newPinValue by remember { mutableStateOf("") }


    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var selectedExportFormat by remember { mutableStateOf("CIPHER") }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Unknown until storage emits. Painted as a blank background rather than
    // guessed, so a rotated screen never shows the wrong theme or toggles.
    val settings by viewModel.uiState.collectAsState(initial = null)

    val loaded = settings
    if (loaded == null) {
        // Storage has not emitted yet. Draw the window background and nothing
        // else, so a rotated screen never flashes a guessed theme or toggle.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }


    val allNotes by viewModel.allNotes.collectAsState(initial = emptyList())
    val pinnedNoteIds by viewModel.pinnedNoteIds.collectAsState(initial = emptySet())

    val context = LocalContext.current
    val isHardwareBiometricAvailable = remember {
        BiometricPromptManager.canAuthenticate(context)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val uriHandler = LocalUriHandler.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let {
            val pass = if (selectedExportFormat == "CIPHER") exportPassword else null
            viewModel.exportBackup(context, it, pass)
            exportPassword = ""
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            viewModel.importBackup(context, it, password = null, onPasswordRequired = {
                showImportPasswordDialog = true
            })
        }
    }


    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportPassword = ""
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Export Backup", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Choose your preferred export format:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedExportFormat = "CIPHER" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedExportFormat == "CIPHER",
                            onClick = { selectedExportFormat = "CIPHER" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Encrypted (.cipher)", fontWeight = FontWeight.SemiBold)
                            Text("Password protected (AES-256)", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedExportFormat = "JSON" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedExportFormat == "JSON",
                            onClick = { selectedExportFormat = "JSON" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Plain Text (.json)", fontWeight = FontWeight.SemiBold)
                            Text("Unencrypted readable format", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                        }
                    }

                    if (selectedExportFormat == "CIPHER") {
                        OutlinedTextField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = { Text("Encryption Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        val fileName = if (selectedExportFormat == "CIPHER") "cipher_notes_backup.cipher" else "cipher_notes_backup.json"
                        exportLauncher.launch(fileName)
                    },
                    enabled = selectedExportFormat == "JSON" || exportPassword.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportPassword = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showImportPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportPasswordDialog = false
                importPassword = ""
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Encrypted Backup", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This file is password protected. Enter the password set during export:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportPasswordDialog = false
                        pendingImportUri?.let { uri ->
                            viewModel.importBackup(context, uri, importPassword, onPasswordRequired = {})
                        }
                        importPassword = ""
                    },
                    enabled = importPassword.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Decrypt & Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportPasswordDialog = false
                    importPassword = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        ListItem(
                            modifier = Modifier.clickable {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            },
                            headlineContent = { Text(mode.label, color = onSurface) },
                            supportingContent = { Text(mode.summary, color = onSurfaceVariant) },
                            leadingContent = { ThemeSwatch(mode, tint = primaryColor) },
                            trailingContent = {
                                RadioButton(selected = mode == loaded.themeMode, onClick = null)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    Text(
                        text = "Dynamic Colors overrides the theme above when available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = primaryColor)
                }
            }
        )
    }

    if (showWidgetNotesPicker) {
        var tempSelectedIds by remember { mutableStateOf(pinnedNoteIds) }

        LaunchedEffect(showWidgetNotesPicker) {
            tempSelectedIds = pinnedNoteIds
        }

        AlertDialog(
            onDismissRequest = { showWidgetNotesPicker = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Select Notes for Widget", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Choose up to 4 notes (${tempSelectedIds.size}/4 selected):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (allNotes.isEmpty()) {
                        Text(
                            "No notes available. Create some notes first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                    } else {
                        allNotes.forEach { note ->
                            val isChecked = tempSelectedIds.contains(note.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSelectedIds = if (isChecked) {
                                            tempSelectedIds - note.id
                                        } else {
                                            if (tempSelectedIds.size < 4) {
                                                tempSelectedIds + note.id
                                            } else {
                                                tempSelectedIds
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = note.title.ifEmpty { "Untitled" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (note.encrypted) "🔒 Encrypted" else note.content.take(40),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSelectedWidgetNotes(tempSelectedIds)
                        showWidgetNotesPicker = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWidgetNotesPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                newPinValue = ""
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Set App PIN", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter a 4-digit PIN to secure your notes on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPinValue,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinValue = it
                        },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinValue.length == 4) {
                            viewModel.setAppPin(newPinValue)
                            viewModel.setAppLock(true)
                            showPinDialog = false
                            newPinValue = ""
                        }
                    },
                    enabled = newPinValue.length == 4,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    newPinValue = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRemovePinConfirm) {
        AlertDialog(
            onDismissRequest = { showRemovePinConfirm = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Remove PIN?") },
            text = { Text("This will disable App Lock and delete your security code. Your notes will no longer be protected by this PIN.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAppPin(null)
                        viewModel.setAppLock(false)
                        showRemovePinConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Nuclear Option", style = MaterialTheme.typography.headlineSmall, color = onSurface)
            },
            text = {
                Text(
                    "This will permanently delete ALL notes and checklists. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.nuclearWipe()
                        showDeleteDialog = false
                        onNuclearWipeComplete()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = primaryColor)
                }
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Open Source Licenses", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    val libs = listOf(
                        "Jetpack Compose" to "Apache License 2.0",
                        "Navigation Compose" to "Apache License 2.0",
                        "Dagger Hilt" to "Apache License 2.0",
                        "Room Database" to "Apache License 2.0",
                        "AndroidX Security-Crypto" to "Apache License 2.0",
                        "Jetpack DataStore" to "Apache License 2.0",
                        "AndroidX Core SplashScreen" to "Apache License 2.0",
                        "Kotlin Coroutines & Flow" to "Apache License 2.0",
                        "AndroidX Biometric" to "Apache License 2.0"
                    )

                    libs.forEach { (name, license) ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(name, style = MaterialTheme.typography.labelLarge, color = primaryColor)
                            Text(license, style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close", color = primaryColor)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Dynamic Colors", color = onSurface) },
                    supportingContent = {
                        Text("Match app colors to your wallpaper (Android 12+)", color = onSurfaceVariant)
                    },
                    leadingContent = { Icon(Icons.Rounded.Palette, null, tint = primaryColor) },
                    trailingContent = {
                        Switch(
                            checked = loaded.dynamicColors,
                            onCheckedChange = { viewModel.setDynamicColors(it) },
                            colors = cipherSwitchColors()
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    modifier = Modifier.clickable { showThemeDialog = true },
                    headlineContent = { Text("Theme", color = onSurface) },
                    supportingContent = { Text(loaded.themeMode.summary, color = onSurfaceVariant) },
                    leadingContent = {
                        ThemeSwatch(loaded.themeMode, tint = primaryColor)
                    },
                    trailingContent = {
                        Text(loaded.themeMode.label, color = primaryColor, style = MaterialTheme.typography.labelLarge)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.labelLarge,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        modifier = Modifier.clickable {
                            showExportDialog = true
                        },
                        headlineContent = { Text("Export Backup", color = onSurface) },
                        supportingContent = { Text("Save backup in .cipher (encrypted) or .json format", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.FileDownload, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    ListItem(
                        modifier = Modifier.clickable {
                            importLauncher.launch("*/*")
                        },
                        headlineContent = { Text("Import Backup", color = onSurface) },
                        supportingContent = { Text("Restore notes from .cipher or .json backup file", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.FileUpload, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Widget",
                style = MaterialTheme.typography.labelLarge,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        modifier = Modifier.clickable { showWidgetNotesPicker = true },
                        headlineContent = { Text("Pinned Notes", color = onSurface) },
                        supportingContent = {
                            Text("Select up to 4 notes (${pinnedNoteIds.size}/4 selected)", color = onSurfaceVariant)
                        },
                        leadingContent = {
                            Icon(imageVector = Icons.Rounded.PushPin, contentDescription = null, tint = primaryColor)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    ListItem(
                        headlineContent = { Text(text = "Show note titles", color = onSurface) },
                        supportingContent = {
                            Text(text = "Note bodies are never shown on the widget", color = onSurfaceVariant)
                        },
                        leadingContent = {
                            Icon(imageVector = Icons.Rounded.Widgets, contentDescription = null, tint = primaryColor)
                        },
                        trailingContent = {
                            Switch(
                                checked = loaded.widgetContentVisible,
                                onCheckedChange = { viewModel.setWidgetContentVisible(it) },
                                colors = cipherSwitchColors()
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Privacy & Safety",
                style = MaterialTheme.typography.labelLarge,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Seal new notes by default", color = onSurface) },
                        supportingContent = {
                            Text("Prompt for a passphrase when a note is created", color = onSurfaceVariant)
                        },
                        leadingContent = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.secondary) },
                        trailingContent = {
                            Switch(
                                checked = loaded.sealNewNotesByDefault,
                                onCheckedChange = { viewModel.setSealNewNotesByDefault(it) },
                                colors = cipherSwitchColors(MaterialTheme.colorScheme.secondary)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    ListItem(
                        headlineContent = { Text("App Lock", color = onSurface) },
                        supportingContent = { Text("Require authentication to open the app", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.Lock, null, tint = primaryColor) },
                        trailingContent = {
                            Switch(
                                checked = loaded.appLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !loaded.appPinSet) {
                                        showPinDialog = true
                                    } else {
                                        viewModel.setAppLock(enabled)
                                    }
                                },
                                colors = cipherSwitchColors()
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    if (loaded.appLockEnabled) {
                        ListItem(
                            headlineContent = { Text("Biometric Unlock", color = onSurface) },
                            supportingContent = {
                                Text(
                                    if (isHardwareBiometricAvailable) "Use fingerprint or face recognition"
                                    else "Biometric authentication not available on this device",
                                    color = onSurfaceVariant
                                )
                            },
                            leadingContent = { Icon(Icons.Rounded.Fingerprint, null, tint = primaryColor) },
                            trailingContent = {
                                Switch(
                                    checked = loaded.biometricEnabled && isHardwareBiometricAvailable,
                                    enabled = isHardwareBiometricAvailable,
                                    onCheckedChange = { viewModel.setBiometric(it) },
                                    colors = cipherSwitchColors()
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            modifier = Modifier.clickable { showPinDialog = true },
                            headlineContent = { Text("Change App PIN", color = onSurface) },
                            supportingContent = {
                                Text(
                                    if (loaded.appPinSet) "Update your 4-digit security code" else "PIN not set",
                                    color = onSurfaceVariant
                                )
                            },
                            leadingContent = { Icon(Icons.Rounded.Password, null, tint = primaryColor) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (loaded.appPinSet) {
                            ListItem(
                                modifier = Modifier.clickable { showRemovePinConfirm = true },
                                headlineContent = { Text("Remove App PIN", color = onSurface) },
                                supportingContent = { Text("Disables lock and clears security code", color = onSurfaceVariant) },
                                leadingContent = { Icon(Icons.Rounded.LockOpen, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    ListItem(
                        modifier = Modifier.clickable { showDeleteDialog = true },
                        headlineContent = {
                            Text("Nuclear Wipe", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        },
                        supportingContent = {
                            Text("Permanently destroy all data", color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.labelLarge,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Encryption", color = onSurface) },
                        supportingContent = { Text("On-device AES-256 GCM encryption", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.Shield, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    ListItem(
                        modifier = Modifier.clickable { showLicensesDialog = true },
                        headlineContent = { Text("Open Source Licenses", color = onSurface) },
                        supportingContent = { Text("Legal information and tech stack", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        modifier = Modifier.clickable { uriHandler.openUri("https://cipherapps.github.io/") },
                        headlineContent = { Text("Project Website", color = onSurface) },
                        supportingContent = { Text("cipherapps.github.io", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Default.Language, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cipher Ember",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Text(
                    text = "Ember Archive · based on CipherNotes 2.3.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant
                )
                Text(
                    text = "© 2026 CipherApps · MIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(mode: ThemeMode, tint: Color) {
    val swatches = themeSwatches(mode)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        swatches.forEach { color ->
            Surface(
                modifier = Modifier.size(20.dp),
                shape = RoundedCornerShape(6.dp),
                color = color,
                border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.35f))
            ) {}
        }
    }
}