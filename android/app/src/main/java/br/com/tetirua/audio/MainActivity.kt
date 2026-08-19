package br.com.tetirua.audio

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.tetirua.audio.core.AudioIds
import br.com.tetirua.audio.core.SynthesisRequest
import br.com.tetirua.audio.core.TtsOutputMode
import br.com.tetirua.audio.tts.PiperSherpaOnnxTtsEngine

/** Aplicação demonstrativa somente de Piper pt_BR + sherpa-onnx. */
class MainActivity : AppCompatActivity() {
    private lateinit var engine: PiperSherpaOnnxTtsEngine
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var input: EditText
    private lateinit var loadButton: Button
    private lateinit var speakButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = PiperSherpaOnnxTtsEngine(this)
        setContentView(createContent())
        loadRuntime()
    }

    private fun createContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        root.addView(TextView(this).apply {
            text = "Tetiruã — Piper + sherpa-onnx"
            textSize = 24f
        }, matchParent())
        root.addView(TextView(this).apply {
            text = "Branch independente de TTS offline. Voz de referência: Piper pt_BR-faber, executada pelo OfflineTts do sherpa-onnx."
            textSize = 16f
        }, matchParent())

        input = EditText(this).apply {
            hint = "Texto para falar"
            setText("Olá. Este é um teste do Tetiruã em português brasileiro.")
            minLines = 4
            gravity = android.view.Gravity.TOP
        }
        root.addView(input, matchParent())

        loadButton = Button(this).apply {
            text = "Carregar Piper"
            setOnClickListener { loadRuntime() }
        }
        root.addView(loadButton, matchParent())

        speakButton = Button(this).apply {
            text = "Gerar e falar"
            isEnabled = false
            setOnClickListener { speak() }
        }
        root.addView(speakButton, matchParent())

        stopButton = Button(this).apply {
            text = "Parar áudio"
            setOnClickListener {
                engine.stop()
                statusText.text = "Estado: reprodução parada"
            }
        }
        root.addView(stopButton, matchParent())

        resultText = TextView(this).apply {
            text = "Resultado: —"
            textSize = 15f
            setPadding(0, 24, 0, 12)
        }
        root.addView(resultText, matchParent())

        statusText = TextView(this).apply {
            text = "Estado: iniciando"
            textSize = 15f
        }
        root.addView(statusText, matchParent())

        return ScrollView(this).apply { addView(root) }
    }

    private fun loadRuntime() {
        loadButton.isEnabled = false
        speakButton.isEnabled = false
        statusText.text = "Estado: carregando Piper + sherpa-onnx"
        Thread {
            runCatching { engine.load() }
                .onSuccess {
                    runOnUiThread {
                        loadButton.isEnabled = true
                        speakButton.isEnabled = true
                        statusText.text = "Estado: pronto para gerar áudio"
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        loadButton.isEnabled = true
                        statusText.text = "Estado: prepare as bibliotecas/modelo — ${error.message}"
                    }
                }
        }.start()
    }

    private fun speak() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) {
            statusText.text = "Estado: informe um texto"
            return
        }

        speakButton.isEnabled = false
        statusText.text = "Estado: gerando WAV"
        engine.speak(
            SynthesisRequest(
                ids = AudioIds(),
                text = text,
                language = "pt-BR",
                outputMode = TtsOutputMode.PLAYBACK,
            ),
        ) { result ->
            runOnUiThread {
                speakButton.isEnabled = true
                resultText.text = buildString {
                    append("Engine: ${result.engine}\n")
                    append("Modelo: ${result.model}\n")
                    append("Arquivo: ${result.audioPath ?: "não gerado"}\n")
                    append("Duração: ${result.durationMs ?: 0} ms")
                    if (result.warnings.isNotEmpty()) {
                        append("\nAviso: ${result.warnings.joinToString()}")
                    }
                }
                statusText.text = if (result.audioPath != null) {
                    "Estado: áudio reproduzido"
                } else {
                    "Estado: falha ao gerar áudio"
                }
            }
        }
    }

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }

    private fun matchParent() = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )
}
