package com.example.ui.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class ArabicTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _ttsStatusMessage = MutableStateFlow<String?>(null)
    val ttsStatusMessage: StateFlow<String?> = _ttsStatusMessage.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("ArabicTtsManager", "Failed to init TTS", e)
            _ttsStatusMessage.value = "محرك النطق الصوتي غير متاح في جهازك حالياً"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val arabicLocale = Locale("ar")
            val result = tts?.setLanguage(arabicLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // محاولة تجربة لغة افتراضية
                val altLocale = Locale.forLanguageTag("ar-SA")
                val altResult = tts?.setLanguage(altLocale)
                if (altResult == TextToSpeech.LANG_MISSING_DATA || altResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    _ttsStatusMessage.value = "يرجى تثبيت بيانات اللغة العربية في إعدادات تحويل النص إلى كلام (TTS) بالهاتف"
                } else {
                    isInitialized = true
                }
            } else {
                isInitialized = true
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        } else {
            _ttsStatusMessage.value = "تعذر تهيئة محرك النطق الصوتي"
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        stop()
        if (!isInitialized || tts == null) {
            _ttsStatusMessage.value = "جاري تهيئة النطق الصوتي العربي..."
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                _ttsStatusMessage.value = "النطق الصوتي غير مدعوم على هذا الجهاز"
            }
            return
        }

        val utteranceId = "ARABIC_IRAB_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e("ArabicTtsManager", "Error stopping TTS", e)
        }
        _isSpeaking.value = false
    }

    fun clearStatus() {
        _ttsStatusMessage.value = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
