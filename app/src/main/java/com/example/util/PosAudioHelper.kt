package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Utility for audio feedback tones on POS hardware.
 */
object PosAudioHelper {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun playScanBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            // Fallback ignore
        }
    }

    fun playSuccessBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (e: Exception) {
            // Fallback ignore
        }
    }

    fun playErrorBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 350)
        } catch (e: Exception) {
            // Fallback ignore
        }
    }
}
