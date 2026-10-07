package dev.cipher.notes.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.cipher.notes.MainActivity
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteRepository
import kotlinx.coroutines.flow.firstOrNull

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotesWidgetEntryPoint {
    fun noteRepository(): NoteRepository
}

class NotesWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val WIDGET_CONTENT_VISIBLE_KEY = booleanPreferencesKey("widget_content_visible")
        val SELECTED_NOTE_IDS_KEY = stringSetPreferencesKey("selected_note_ids")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            NotesWidgetEntryPoint::class.java
        )

        val repository = entryPoint.noteRepository()

        val allNotes = runCatching {
            repository.getAllNotes().firstOrNull() ?: emptyList()
        }.getOrDefault(emptyList())


        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            val state = currentState<Preferences>()
            val selectedNoteIds = state[SELECTED_NOTE_IDS_KEY] ?: emptySet()
            val isContentVisible = state[WIDGET_CONTENT_VISIBLE_KEY] ?: false

            val displayedNotes = allNotes
                .filter { note -> selectedNoteIds.contains(note.id) }
                .take(4)

            GlanceTheme {
                WidgetContent(
                    notes = displayedNotes,
                    isTitleVisible = isContentVisible,
                    clickIntent = mainActivityIntent
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(notes: List<Note>, isTitleVisible: Boolean, clickIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color(0xFF14100D), night = Color(0xFF14100D)))
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(actionStartActivity(intent = clickIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pinned Notes",
                    style = TextStyle(
                        color = ColorProvider(day = Color(0xFFD9A441), night = Color(0xFFD9A441)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (notes.isEmpty()) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(clickIntent)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No notes pinned\nSelect in settings",
                        style = TextStyle(
                            color = ColorProvider(day = Color.Gray, night = Color.Gray),
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    items(
                        items = notes,
                        itemId = { note -> note.id.hashCode().toLong() }
                    ) { note ->
                        NoteWidgetItem(
                            note = note,
                            isTitleVisible = isTitleVisible,
                            clickIntent = clickIntent
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun NoteWidgetItem(note: Note, isTitleVisible: Boolean, clickIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(ColorProvider(day = Color(0xFF221C16), night = Color(0xFF221C16)))
                .padding(8.dp)
                .clickable(actionStartActivity(clickIntent))
        ) {
            // The home screen (and lock screen) can be read by anyone holding the
            // device, so the widget never renders note bodies. Only the title is
            // ever shown, and only when the user opts in.
            Text(
                text = WidgetText.title(note.title, isTitleVisible),
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFFF3EAE0), night = Color(0xFFF3EAE0)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = WidgetText.status(note.encrypted),
                style = TextStyle(
                    color = ColorProvider(
                        day = if (note.encrypted) Color(0xFFD9A441) else Color(0xFF8A7A6A),
                        night = if (note.encrypted) Color(0xFFD9A441) else Color(0xFF8A7A6A)
                    ),
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}