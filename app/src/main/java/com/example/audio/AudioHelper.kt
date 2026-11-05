package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(): File? {
        try {
            val audioDir = File(context.cacheDir, "b13_audio_notes").apply { mkdirs() }
            val file = File(audioDir, "VOICE_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            startTimeMs = System.currentTimeMillis()
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            currentOutputFile = null
            return null
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        val file = currentOutputFile
        var durationSec = 0
        try {
            if (startTimeMs > 0) {
                durationSec = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt().coerceAtLeast(1)
            }
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to stop recording", e)
        } finally {
            mediaRecorder = null
            currentOutputFile = null
            startTimeMs = 0L
        }
        return Pair(file, durationSec)
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            currentOutputFile?.delete()
        } catch (_: Exception) {}
        finally {
            mediaRecorder = null
            currentOutputFile = null
            startTimeMs = 0L
        }
    }
}

class AudioPlayerHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath = _currentPlayingPath.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playProgress = MutableStateFlow(0f)
    val playProgress = _playProgress.asStateFlow()

    fun togglePlay(filePath: String) {
        if (_currentPlayingPath.value == filePath && _isPlaying.value) {
            pause()
        } else if (_currentPlayingPath.value == filePath && !_isPlaying.value) {
            resume()
        } else {
            play(filePath)
        }
    }

    fun play(filePath: String) {
        stop()
        val file = File(filePath)
        if (!file.exists()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playProgress.value = 0f
                    progressJob?.cancel()
                }
                start()
            }
            _currentPlayingPath.value = filePath
            _isPlaying.value = true

            startProgressTracker()
        } catch (e: IOException) {
            Log.e("AudioPlayerHelper", "Playback error", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
        progressJob?.cancel()
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingPath.value = null
        _playProgress.value = 0f
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer != null && _isPlaying.value) {
                try {
                    val current = mediaPlayer?.currentPosition ?: 0
                    val total = mediaPlayer?.duration ?: 1
                    if (total > 0) {
                        _playProgress.value = current.toFloat() / total.toFloat()
                    }
                } catch (_: Exception) {}
                delay(100)
            }
        }
    }
}
