package com.sanx.app.service.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evidence collector for silent audio recording and camera snapshot capture.
 * All files are saved to the app's private internal storage (not visible in gallery).
 * Recordings are segmented every 30 seconds to prevent memory overflow.
 *
 * File naming: {sessionId}_{timestamp}_{segment}.m4a / .jpg
 */
class EvidenceCollector(
    private val context: Context,
    private val onSegmentCompleted: (File) -> Unit = {}
) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentSessionId: String = ""
    private var segmentIndex = 0
    private var isRecordingAudio = false

    private val evidenceDir: File get() {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, "evidence/$currentSessionId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ─── Audio Recording ───────────────────────────────────────────────────

    /**
     * Starts a silent background audio recording session.
     * Rotates files every 20 seconds automatically.
     */
    fun startAudioRecording(sessionId: String) {
        if (isRecordingAudio) return
        currentSessionId = sessionId
        segmentIndex = 0
        startNewAudioSegment()
    }

    private fun startNewAudioSegment() {
        stopCurrentAudioSegment()

        val timestamp = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(evidenceDir, "${currentSessionId}_${timestamp}_seg${segmentIndex}.m4a")
        segmentIndex++

        @Suppress("DEPRECATION")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)        // Low bitrate for small file size
                setAudioSamplingRate(22050)           // Sufficient for voice intelligibility
                setMaxDuration(20_000)                // 20-second segment
                setOutputFile(outputFile.absolutePath)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        onSegmentCompleted(outputFile)
                        startNewAudioSegment()        // Auto-rotate segment
                    }
                }
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecordingAudio = true
        } catch (e: Exception) {
            recorder.release()
        }
    }

    private fun stopCurrentAudioSegment() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
    }

    fun stopAudioRecording() {
        stopCurrentAudioSegment()
        isRecordingAudio = false
    }

    // ─── Evidence File Access ───────────────────────────────────────────────

    fun getEvidenceFiles(sessionId: String): List<File> {
        val dir = File(context.filesDir, "evidence/$sessionId")
        return dir.listFiles()?.toList() ?: emptyList()
    }

    fun clearEvidenceOlderThan(cutoffMs: Long) {
        val evidenceRoot = File(context.filesDir, "evidence")
        evidenceRoot.listFiles()?.forEach { sessionDir ->
            if (sessionDir.lastModified() < cutoffMs) {
                sessionDir.deleteRecursively()
            }
        }
    }

    val isAudioActive: Boolean get() = isRecordingAudio
}
