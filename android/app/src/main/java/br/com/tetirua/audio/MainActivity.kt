package br.com.tetirua.audio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.tetirua.audio.core.AudioIds
import br.com.tetirua.audio.core.AudioPipeline
import br.com.tetirua.audio.core.AudioPipelineState
import br.com.tetirua.audio.core.SynthesisRequest
import br.com.tetirua.audio.core.TranscriptionRequest
import br.com.tetirua.audio.core.TtsOutputMode
import br.com.tetirua.audio.stt.SimulatedSttEngine
import br.com.tetirua.audio.tts.AndroidTtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var ttsEngine: AndroidTtsEngine
    private lateinit var statusText: TextView
    private lateinit var questionInput: EditText
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsEngine = AndroidTtsEngine(this)
        setContentView(createContent())

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION,
            )
        }
    }

    private fun createContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Tetiruã — teste de áudio"
            textSize = 24f
        }
        root.addView(title, matchParent())

        val description = TextView(this).apply {
            text = "Simulação: wake word → VAD → STT → resposta falada pelo TTS."
            textSize = 16f
        }
        root.addView(description, matchParent())

        questionInput = EditText(this).apply {
            hint = "Digite a resposta que deseja testar no áudio"
            setText("Este é um teste do Tetiruã no celular.")
            minLines = 3
            gravity = android.view.Gravity.TOP
        }
        root.addView(questionInput, matchParent())

        val runButton = Button(this).apply {
            text = "Simular wake word e falar"
            setOnClickListener { runAudioSimulation() }
        }
        root.addView(runButton, matchParent())

        val stopButton = Button(this).apply {
            text = "Parar áudio"
            setOnClickListener {
                ttsEngine.stop()
                statusText.text = "Estado: reprodução cancelada"
            }
        }
        root.addView(stopButton, matchParent())

        statusText = TextView(this).apply {
            text = "Estado: LOW_POWER_LISTENING"
            textSize = 15f
        }
        root.addView(statusText, matchParent())

        return ScrollView(this).apply { addView(root) }
    }

    private fun runAudioSimulation() {
        val answerText = questionInput.text.toString().trim()
        if (answerText.isEmpty()) {
            statusText.text = "Estado: nenhuma resposta para reproduzir"
            return
        }

        val ids = AudioIds()
        val pipeline = AudioPipeline(
            stt = SimulatedSttEngine(),
            tts = ttsEngine,
            onStateChanged = { state ->
                runOnUiThread { statusText.text = "Estado: ${state.name}" }
            },
        )

        statusText.text = "Estado: WAKE_WORD_CONFIRMED"
        activityScope.launch {
            pipeline.transcribeAndSpeak(
                transcriptionRequest = TranscriptionRequest(
                    ids = ids,
                    audioPath = "simulated://microphone/turn-${ids.turnId}",
                    languageHint = "pt-BR",
                ),
                answerText = answerText,
                synthesisRequestFactory = { requestIds, text ->
                    SynthesisRequest(
                        ids = requestIds,
                        text = text,
                        language = "pt-BR",
                        outputMode = TtsOutputMode.PLAYBACK,
                    )
                },
                onResult = { transcription, synthesis ->
                    runOnUiThread {
                        statusText.text = buildString {
                            append("Estado: COMPLETED\n")
                            append("STT: ${transcription.text}\n")
                            append("TTS: ${synthesis.engine}")
                            if (synthesis.warnings.isNotEmpty()) {
                                append("\nAviso: ${synthesis.warnings.joinToString()}")
                            }
                        }
                    }
                },
            )
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        ttsEngine.release()
        super.onDestroy()
    }

    private fun matchParent() = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 1001
    }
}
