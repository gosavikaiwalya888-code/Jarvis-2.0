
package com.jarvis.assistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var speaker: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speaker = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                speaker.language = Locale.UK
            }
        }

        setContent {
            JarvisScreen(
                onSpeak = { speak(it) },
                onCommand = { handleCommand(it) },
                onStartNotch = { startNotch() },
                onNotificationAccess = {
                    openNotificationAccess()
                }
            )
        }
    }

    private fun openNotificationAccess() {
        val intent = Intent(
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        )
        startActivity(intent)
    }

    private fun startNotch() {
        if (Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(
                this,
                FloatingNotchService::class.java
            )
            startForegroundService(serviceIntent)
        } else {
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(permissionIntent)
        }
    }

    private fun speak(message: String) {
        speaker.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS"
        )
    }

    private fun handleCommand(command: String) {
        val text = command.lowercase(Locale.getDefault())

        when {
            "bluetooth" in text ->
                startActivity(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                )

            "settings" in text ->
                startActivity(
                    Intent(Settings.ACTION_SETTINGS)
                )

            "alarm" in text ->
                startActivity(
                    Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
                )

            "call" in text -> {
                speak("Opening phone dialer.")
                startActivity(Intent(Intent.ACTION_DIAL))
            }

            "message" in text || "sms" in text ->
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("smsto:"))
                )

            "whatsapp" in text -> {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, "Hello!")
                }

                runCatching {
                    startActivity(intent)
                }.onFailure {
                    speak("WhatsApp is unavailable.")
                }
            }

            "gmail" in text || "email" in text ->
                startActivity(
                    Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:")
                    )
                )

            "stock" in text || "market" in text ->
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/finance/")
                    )
                )

            "hello" in text || "hi jarvis" in text ->
                speak("Hello sir. JARVIS systems are ready.")

            else ->
                speak("I heard you. This command is not available yet.")
        }
    }

    override fun onDestroy() {
        if (::speaker.isInitialized) {
            speaker.stop()
            speaker.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
fun JarvisScreen(
    onSpeak: (String) -> Unit,
    onCommand: (String) -> Unit,
    onStartNotch: () -> Unit,
    onNotificationAccess: () -> Unit
) {
    var status by remember {
        mutableStateOf("SYSTEM ONLINE")
    }

    var command by remember {
        mutableStateOf("")
    }

    val blue = Color(0xFF00BFFF)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
            ?.firstOrNull()

        if (spoken != null) {
            command = spoken
            status = "COMMAND RECEIVED"
            onCommand(spoken)
        } else {
            status = "READY"
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(primary = blue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050B16))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(25.dp))

            Text(
                "J.A.R.V.I.S.",
                color = blue,
                fontSize = 32.sp
            )

            Text(
                "PERSONAL AI ASSISTANT",
                color = Color.LightGray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(30.dp))

            Surface(
                modifier = Modifier.size(170.dp),
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFF102B48)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "J",
                        color = blue,
                        fontSize = 76.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(status, color = blue)

            Spacer(Modifier.height(15.dp))

            Button(
                onClick = {
                    status = "LISTENING..."

                    val intent = Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    ).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE,
                            "en-IN"
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            "Yes sir?"
                        )
                    }

                    try {
                        launcher.launch(intent)
                    } catch (_: Exception) {
                        status = "VOICE SERVICE UNAVAILABLE"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🎙 TALK TO JARVIS")
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onStartNotch,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ENABLE FLOATING NOTCH")
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onNotificationAccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ENABLE NOTIFICATION ACCESS")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("Enter a command") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (command.isNotBlank()) {
                        onCommand(command)
                        status = "COMMAND SENT"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("EXECUTE COMMAND")
            }

            Spacer(Modifier.height(20.dp))

            Text("QUICK ACCESS", color = blue)

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickButton("Settings", Modifier.weight(1f)) {
                    onCommand("settings")
                }

                QuickButton("Alarm", Modifier.weight(1f)) {
                    onCommand("alarm")
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickButton("Call", Modifier.weight(1f)) {
                    onCommand("call")
                }

                QuickButton("WhatsApp", Modifier.weight(1f)) {
                    onCommand("whatsapp")
                }

                QuickButton("Stocks", Modifier.weight(1f)) {
                    onCommand("stock")
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Microphone activates only when you tap Talk.",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QuickButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(5.dp)
    ) {
        Text(label, fontSize = 11.sp)
    }
}
