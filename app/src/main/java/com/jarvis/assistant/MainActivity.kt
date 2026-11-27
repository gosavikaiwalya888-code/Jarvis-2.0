
package com.jarvis.assistant

import android.app.AlertDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    private var startNotchAfterPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speaker = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val languageResult = speaker.setLanguage(Locale.UK)
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    speaker.setLanguage(Locale.ENGLISH)
                }
                speaker.setSpeechRate(0.95f)
            }
        }

        setContent {
            JarvisScreen(
                onSpeak = { speak(it) },
                onCommand = { handleCommand(it) },
                onStartNotch = { startNotch() },
                onNotificationAccess = { openNotificationAccess() }
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (startNotchAfterPermission) {
            startNotchAfterPermission = false
            if (Settings.canDrawOverlays(this)) {
                launchNotchService()
            }
        }
    }

    private fun speak(message: String) {
        if (::speaker.isInitialized) {
            speaker.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS"
            )
        }
    }

    private fun openNotificationAccess() {
        startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        )
    }

    private fun startNotch() {
        if (Settings.canDrawOverlays(this)) {
            launchNotchService()
        } else {
            startNotchAfterPermission = true
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(permissionIntent)
        }
    }

    private fun launchNotchService() {
        try {
            val serviceIntent = Intent(
                this,
                FloatingNotchService::class.java
            )
            startForegroundService(serviceIntent)
            speak("Floating interface activated.")
        } catch (_: Exception) {
            speak("I could not start the floating interface.")
        }
    }

    private fun openApp(packageName: String, appName: String) {
        val launchIntent = packageManager
            .getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            speak("$appName is not installed.")
        }
    }

    private fun searchWeb(query: String) {
        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun confirmOpenDialer() {
        AlertDialog.Builder(this)
            .setTitle("JARVIS confirmation")
            .setMessage("Would you like me to open the phone dialer?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open dialer") { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL))
            }
            .show()
    }

    private fun handleCommand(command: String) {
        val text = command.lowercase(Locale.UK).trim()

        when {
            text.contains("bluetooth") -> {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }

            text.contains("settings") -> {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }

            text.contains("alarm") -> {
                startActivity(
                    Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
                )
            }

            text.contains("call") || text.contains("phone dialer") -> {
                confirmOpenDialer()
            }

            text.contains("whatsapp") -> {
                openApp("com.whatsapp", "WhatsApp")
            }

            text.contains("famapp") || text.contains("fampay") -> {
                searchWeb("FamApp official app")
            }

            text.contains("discount") ||
            text.contains("shopping deal") ||
            text.contains("coupon") -> {
                searchWeb(
                    "latest shopping discounts coupons India Amazon Flipkart Myntra"
                )
            }

            text.contains("amazon") -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.amazon.in/")
                    )
                )
            }

            text.contains("flipkart") -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.flipkart.com/")
                    )
                )
            }

            text.contains("gmail") || text.contains("email") -> {
                startActivity(
                    Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:")
                    )
                )
            }

            text.contains("weather") -> {
                searchWeb("weather today")
            }

            text.contains("time") -> {
                val currentTime = java.text.SimpleDateFormat(
                    "h:mm a",
                    Locale.UK
                ).format(java.util.Date())
                speak("The time is $currentTime.")
            }

            text.contains("date") -> {
                val currentDate = java.text.SimpleDateFormat(
                    "EEEE, d MMMM yyyy",
                    Locale.UK
                ).format(java.util.Date())
                speak("Today is $currentDate.")
            }

            text.startsWith("search ") -> {
                searchWeb(text.removePrefix("search "))
            }

            text.contains("hello") || text.contains("hi jarvis") -> {
                speak("Good day, sir. JARVIS is ready.")
            }

            else -> {
                speak(
                    "I have not learned that command yet. " +
                    "You can ask me to open apps, set an alarm, " +
                    "check the time, or find shopping discounts."
                )
            }
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
    var status by remember { mutableStateOf("SYSTEM ONLINE") }
    var command by remember { mutableStateOf("") }

    val blue = Color(0xFF00BFFF)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        if (!spoken.isNullOrBlank()) {
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
                .verticalScroll(rememberScrollState())
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

            Spacer(Modifier.height(25.dp))

            Surface(
                modifier = Modifier.size(150.dp),
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFF102B48)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("J", color = blue, fontSize = 76.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
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
                            "en-GB"
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
                Text("ENABLE FLOATING INTERFACE")
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onNotificationAccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ENABLE NOTIFICATION ACCESS")
            }

            Spacer(Modifier.height(15.dp))

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

            Button(
                onClick = { onCommand("settings") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SETTINGS")
            }

            Button(
                onClick = { onCommand("alarm") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ALARM")
            }

            Button(
                onClick = { onCommand("whatsapp") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("WHATSAPP")
            }

            Button(
                onClick = { onCommand("famapp") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("FAMAPP")
            }

            Button(
                onClick = { onCommand("shopping discounts") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SHOPPING DEALS")
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "Microphone activates only when you tap Talk.",
                color = Color.Gray,
                fontSize = 11.sp
            )

            Text(
                "British English voice depends on your phone's installed voice engine.",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}
