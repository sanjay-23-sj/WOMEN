package com.sanx.app.service.trigger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.sanx.app.data.model.TriggerSensitivity
import java.util.Locale

/**
 * Continuous background Speech Recognizer for detection of secret emergency trigger phrases.
 * Runs on the main UI thread via a main handler.
 * Automatically recovers from timeouts, audio interrupts, and other common speech API errors.
 */
class VoicePhraseDetector(
    private val context: Context,
    private val sensitivity: TriggerSensitivity,
    private val onVoicePhraseDetected: () -> Unit
) : RecognitionListener {

    companion object {
        private const val TAG = "VoicePhraseDetector"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isListening = false

    fun start() {
        mainHandler.post {
            if (isListening) return@post
            isListening = true
            initRecognizerAndStart()
        }
    }

    fun stop() {
        mainHandler.post {
            if (!isListening) return@post
            isListening = false
            cleanupRecognizer()
        }
    }

    private fun initRecognizerAndStart() {
        try {
            cleanupRecognizer()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "Speech recognition not available on this device")
                return
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoicePhraseDetector)
            }

            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(recognizerIntent)
            Log.d(TAG, "Speech recognizer started listening successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer, retrying in 3 seconds", e)
            scheduleRestart(3000L)
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isListening) {
                initRecognizerAndStart()
            }
        }, delayMs)
    }

    private fun restartListening() {
        mainHandler.post {
            if (!isListening) return@post
            try {
                speechRecognizer?.startListening(recognizerIntent)
                Log.d(TAG, "Restarted speech recognition listening")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening directly, recreating recognizer", e)
                initRecognizerAndStart()
            }
        }
    }

    // ─── RecognitionListener Callbacks ───────────────────────────────────────────

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    
    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
    }

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input timeout"
            else -> "Unknown error"
        }
        Log.d(TAG, "Speech recognizer error: $error ($message)")

        // For busy/perms errors, delay slightly longer. For normal timeouts, restart immediately.
        val delay = when (error) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1000L
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 5000L
            else -> 300L
        }
        scheduleRestart(delay)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null) {
            processMatches(matches)
        }
        restartListening()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null) {
            processMatches(matches)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun processMatches(matches: ArrayList<String>) {
        val triggerPhrases = when (sensitivity) {
            TriggerSensitivity.HIGH -> listOf("help", "emergency", "save me", "danger", "call uncle", "uncle", "please", "police")
            TriggerSensitivity.MEDIUM -> listOf("help", "emergency", "save me", "danger", "call uncle")
            TriggerSensitivity.LOW -> listOf("emergency", "danger", "call uncle")
        }
        for (match in matches) {
            val lowercaseMatch = match.lowercase(Locale.getDefault())
            Log.d(TAG, "Recognized phrase segment: $lowercaseMatch")
            for (phrase in triggerPhrases) {
                if (lowercaseMatch.contains(phrase)) {
                    Log.i(TAG, "CONFIRMED SECRET VOICE PHRASE DETECTED: $phrase")
                    onVoicePhraseDetected()
                    return
                }
            }
        }
    }
}
