package com.noteification.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var store: NoteStore
    private lateinit var notifier: NoteNotifier
    private var text by mutableStateOf("")

    private val handler = Handler(Looper.getMainLooper())
    private val syncRunnable = Runnable { notifier.sync(text) }

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) notifier.sync(text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = NoteStore(applicationContext)
        notifier = NoteNotifier(applicationContext)
        text = store.load()
        notifier.sync(text) // make sure the notification matches the saved note

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            NoteificationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteScreen(
                        text = text,
                        onTextChange = ::onTextChanged,
                        onDismiss = {
                            onTextChanged("")
                            // focus returns to the box so a new note can start immediately
                        }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Leaving the app without hitting Done still saves and pins.
        handler.removeCallbacks(syncRunnable)
        store.save(text)
        notifier.sync(text)
    }

    private fun onTextChanged(newText: String) {
        text = newText
        store.save(newText) // apply() is async; cheap enough per keystroke
        handler.removeCallbacks(syncRunnable)
        handler.postDelayed(syncRunnable, 400) // debounce notification updates
    }
}

@Composable
fun NoteificationTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF3B82F6))
    } else {
        lightColorScheme(primary = Color(0xFF2563EB))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun NoteScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val hasText = text.isNotBlank()

    fun closeKeyboard() {
        keyboard?.hide()
        focusManager.clearFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Note-ification",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        )
        HorizontalDivider()

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(18.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Type your note…") },
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { closeKeyboard() })
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onDismiss()
                    focusRequester.requestFocus()
                },
                enabled = hasText,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Dismiss")
            }
            Button(
                onClick = { closeKeyboard() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Done")
            }
        }

        Text(
            text = if (hasText) "● Pinned — autosaved, updates live as you type" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}
