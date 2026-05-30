package com.kbko.omniassistant

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import java.util.Locale

// ============ MAIN ACTIVITY ============
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startForegroundService(
            Intent(this, OmniAssistantService::class.java)
        )
        finish()
    }
}

// ============ MAIN SERVICE ============
class OmniAssistantService : Service(), SpeechCallback, TtsCallback {

    private lateinit var speechManager: SpeechManager
    private lateinit var ttsManager: TtsManager
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "omni_assistant_channel"
    private val TAG = "OmniAssistantService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OmniAssistantService created")
        
        initializeNotification()
        initializeManagers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OmniAssistantService started")
        speechManager.startListening()
        return START_STICKY
    }

    // ============ INITIALIZATION ============
    private fun initializeNotification() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OmniAssistant")
            .setContentText("Voice Engine Active: Listening...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION_ID, notification, Service.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OmniAssistant Engine",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun initializeManagers() {
        speechManager = SpeechManager(this)
        speechManager.setSpeechCallback(this)

        ttsManager = TtsManager(this)
        ttsManager.setTtsCallback(this)
    }

    // ============ SPEECH CALLBACKS ============
    override fun onReady() {
        Log.d(TAG, "Speech recognition ready")
        ttsManager.speak("OmniAssistant initialized. Ready to assist.")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Speech detected")
    }

    override fun onSoundLevelChanged(rmsdB: Float) {
        // Audio level monitoring
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "Speech ended")
    }

    override fun onResults(results: List<RecognitionResult>) {
        val bestMatch = results.firstOrNull()?.text ?: return
        Log.d(TAG, "Speech recognized: $bestMatch")
        
        parseAndExecuteCommand(bestMatch)
    }

    override fun onPartialResults(results: List<String>) {
        Log.d(TAG, "Partial results: ${results.firstOrNull()}")
    }

    override fun onListeningStarted() {
        Log.d(TAG, "Listening started")
    }

    override fun onListeningStopped() {
        Log.d(TAG, "Listening stopped")
    }

    override fun onError(error: Int, errorMessage: String) {
        Log.e(TAG, "Speech error: $errorMessage")
        ttsManager.speak("Sorry, I didn't catch that. Please try again.")
    }

    // ============ TTS CALLBACKS ============
    override fun onTtsReady() {
        Log.d(TAG, "Text-to-Speech ready")
    }

    override fun onTtsError(error: String) {
        Log.e(TAG, "TTS Error: $error")
    }

    override fun onSpeakingStarted(utteranceId: String?) {
        Log.d(TAG, "Speaking started: $utteranceId")
    }

    override fun onSpeakingCompleted(utteranceId: String?) {
        Log.d(TAG, "Speaking completed: $utteranceId")
        // Continue listening after speaking
        speechManager.startListening()
    }

    override fun onSpeakingStopped() {
        Log.d(TAG, "Speaking stopped")
    }

    // ============ COMMAND PARSING ============
    private fun parseAndExecuteCommand(command: String) {
        val lowerCommand = command.lowercase()

        when {
            lowerCommand.contains("hello") || lowerCommand.contains("hi") -> {
                ttsManager.speak("Hello! How can I assist you today?")
            }
            lowerCommand.contains("time") -> {
                val time = getCurrentTime()
                ttsManager.speak("The current time is $time")
            }
            lowerCommand.contains("date") -> {
                val date = getCurrentDate()
                ttsManager.speak("Today is $date")
            }
            lowerCommand.contains("stop") || lowerCommand.contains("quit") -> {
                ttsManager.speak("Goodbye! Omni Assistant shutting down.")
                stopSelf()
            }
            lowerCommand.contains("help") -> {
                ttsManager.speak("I can tell you the time, date, or help you with basic tasks. What would you like?")
            }
            else -> {
                ttsManager.speak("I understood: $command. What would you like me to do?")
            }
        }
    }

    private fun getCurrentTime(): String {
        return android.text.format.DateFormat.format("hh:mm a", System.currentTimeMillis()).toString()
    }

    private fun getCurrentDate(): String {
        return android.text.format.DateFormat.format("EEEE, MMMM d, yyyy", System.currentTimeMillis()).toString()
    }

    // ============ LIFECYCLE ============
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "OmniAssistantService destroyed")
        speechManager.destroy()
        ttsManager.destroy()
        super.onDestroy()
    }
}

// ============ SPEECH MANAGER ============
class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var audioManager: AudioManager? = null
    private var isListening = false
    private var speechCallback: SpeechCallback? = null
    private val TAG = "SpeechManager"

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initializeSpeechRecognizer()
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(SpeechRecognitionListener())
            Log.d(TAG, "SpeechRecognizer initialized")
        } else {
            Log.w(TAG, "Speech recognition not available")
        }
    }

    fun startListening() {
        if (isListening || speechRecognizer == null) return

        isListening = true
        requestAudioFocus()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000)
        }

        speechRecognizer?.startListening(intent)
        speechCallback?.onListeningStarted()
        Log.d(TAG, "Started listening")
    }

    fun stopListening() {
        if (!isListening) return

        isListening = false
        speechRecognizer?.stopListening()
        releaseAudioFocus()
        speechCallback?.onListeningStopped()
        Log.d(TAG, "Stopped listening")
    }

    fun setSpeechCallback(callback: SpeechCallback) {
        this.speechCallback = callback
    }

    private fun requestAudioFocus() {
        audioManager?.requestAudioFocus(
            null,
            AudioManager.STREAM_MIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
    }

    private fun releaseAudioFocus() {
        audioManager?.abandonAudioFocus(null)
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d(TAG, "SpeechManager destroyed")
    }

    private inner class SpeechRecognitionListener : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            speechCallback?.onReady()
        }

        override fun onBeginningOfSpeech() {
            speechCallback?.onBeginningOfSpeech()
        }

        override fun onRmsChanged(rmsdB: Float) {
            speechCallback?.onSoundLevelChanged(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            speechCallback?.onEndOfSpeech()
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error"
            }
            speechCallback?.onError(error, errorMessage)
            Log.e(TAG, "Speech error: $errorMessage")

            if (isListening) {
                startListening()
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

            if (!matches.isNullOrEmpty()) {
                val recognitionResults = matches.mapIndexed { index, text ->
                    RecognitionResult(
                        text = text,
                        confidence = confidence?.getOrNull(index) ?: 0f
                    )
                }
                speechCallback?.onResults(recognitionResults)
                Log.d(TAG, "Results: ${matches.firstOrNull()}")
            }

            if (isListening) {
                startListening()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                speechCallback?.onPartialResults(matches)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}

// ============ TTS MANAGER ============
class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private var isReady = false
    private var ttsCallback: TtsCallback? = null
    private val TAG = "TtsManager"

    private val utteranceQueue = mutableListOf<UtteranceData>()
    private var isProcessing = false

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initializeTextToSpeech()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
        Log.d(TAG, "TextToSpeech initialization started")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            configureTextToSpeech()
            ttsCallback?.onTtsReady()
            Log.d(TAG, "TextToSpeech initialized successfully")
            processQueue()
        } else {
            isReady = false
            ttsCallback?.onTtsError("TextToSpeech initialization failed")
            Log.e(TAG, "TextToSpeech initialization failed with status: $status")
        }
    }

    private fun configureTextToSpeech() {
        textToSpeech?.apply {
            language = Locale.getDefault()
            setPitch(1.0f)
            setSpeechRate(1.0f)
            setOnUtteranceProgressListener(UtteranceListener())
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (text.isEmpty()) {
            Log.w(TAG, "Empty text provided to speak()")
            return
        }

        if (isReady) {
            performSpeak(text, queueMode)
        } else {
            utteranceQueue.add(UtteranceData(text, queueMode))
            Log.d(TAG, "TTS not ready. Queued: $text")
        }
    }

    private fun performSpeak(text: String, queueMode: Int) {
        requestAudioFocus()

        val utteranceId = "utterance_${System.currentTimeMillis()}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(text, queueMode, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            textToSpeech?.speak(text, queueMode, HashMap())
        }

        Log.d(TAG, "Speaking: $text (ID: $utteranceId)")
    }

    fun stop() {
        textToSpeech?.stop()
        releaseAudioFocus()
        utteranceQueue.clear()
        isProcessing = false
        ttsCallback?.onSpeakingStopped()
        Log.d(TAG, "TTS stopped")
    }

    fun setPitch(pitch: Float) {
        if (pitch < 0.5f || pitch > 2.0f) {
            Log.w(TAG, "Pitch out of range [0.5-2.0]: $pitch")
            return
        }
        textToSpeech?.setPitch(pitch)
        Log.d(TAG, "Pitch set to: $pitch")
    }

    fun setSpeechRate(rate: Float) {
        if (rate < 0.5f || rate > 2.0f) {
            Log.w(TAG, "Speech rate out of range [0.5-2.0]: $rate")
            return
        }
        textToSpeech?.setSpeechRate(rate)
        Log.d(TAG, "Speech rate set to: $rate")
    }

    fun setLanguage(locale: Locale) {
        val result = textToSpeech?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: $locale")
            ttsCallback?.onTtsError("Language not supported: $locale")
        } else {
            Log.d(TAG, "Language set to: $locale")
        }
    }

    fun setTtsCallback(callback: TtsCallback) {
        this.ttsCallback = callback
    }

    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager?.requestAudioFocus(
                AudioManager.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .build()
                    )
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager?.abandonAudioFocusRequest(
                AudioManager.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build()
            )
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    private fun processQueue() {
        if (isProcessing || utteranceQueue.isEmpty()) return

        isProcessing = true
        val utterance = utteranceQueue.removeAt(0)
        performSpeak(utterance.text, utterance.queueMode)
    }

    private inner class UtteranceListener : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            Log.d(TAG, "Speech started: $utteranceId")
            ttsCallback?.onSpeakingStarted(utteranceId)
        }

        override fun onDone(utteranceId: String?) {
            Log.d(TAG, "Speech completed: $utteranceId")
            ttsCallback?.onSpeakingCompleted(utteranceId)
            releaseAudioFocus()

            if (utteranceQueue.isNotEmpty()) {
                processQueue()
            } else {
                isProcessing = false
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            val errorMessage = when (errorCode) {
                TextToSpeech.ERROR_SYNTHESIS -> "Synthesis error"
                TextToSpeech.ERROR_SERVICE -> "Service error"
                TextToSpeech.ERROR_INVALID_REQUEST -> "Invalid request"
                TextToSpeech.ERROR_NETWORK -> "Network error"
                TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                else -> "Unknown error code: $errorCode"
            }

            Log.e(TAG, "Speech error ($utteranceId): $errorMessage")
            ttsCallback?.onTtsError("Speech error: $errorMessage")

            isProcessing = false
            releaseAudioFocus()
        }
    }

    fun destroy() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isReady = false
        Log.d(TAG, "TtsManager destroyed")
    }
}

// ============ DATA CLASSES ============
data class RecognitionResult(
    val text: String,
    val confidence: Float
)

data class UtteranceData(
    val text: String,
    val queueMode: Int
)

// ============ CALLBACK INTERFACES ============
interface SpeechCallback {
    fun onReady() {}
    fun onBeginningOfSpeech() {}
    fun onSoundLevelChanged(rmsdB: Float) {}
    fun onEndOfSpeech() {}
    fun onError(error: Int, errorMessage: String) {}
    fun onResults(results: List<RecognitionResult>) {}
    fun onPartialResults(results: List<String>) {}
    fun onListeningStarted() {}
    fun onListeningStopped() {}
}

interface TtsCallback {
    fun onTtsReady() {}
    fun onTtsError(error: String) {}
    fun onSpeakingStarted(utteranceId: String?) {}
    fun onSpeakingCompleted(utteranceId: String?) {}
    fun onSpeakingStopped() {}
}
