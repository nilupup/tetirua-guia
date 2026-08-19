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
import br.com.tetirua.audio.tts.KokoroTtsEngine

/** Aplicação demonstrativa somente de Kokoro-82M + sherpa-onnx. */
class MainActivity : AppCompatActivity() {
    private lateinit var engine: KokoroTtsEngine
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var input: EditText
    private lateinit var loadButton: Button
    private lateinit var speakButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = KokoroTtsEngine(this)
        setContentView(createContent())
        loadRuntime()
    }

    private fun createContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        root.addView(TextView(this).apply {
            text = "Tetiruã — Kokoro-82M"
            textSize = 24f
        }, matchParent())
        root.addView(TextView(this).apply {
            text = "Branch independente de TTS offline. Modelo int8 v1.1, speaker af_maple (sid 0), usando sherpa-onnx somente nesta branch. A versão oficial consultada suporta inglês e chinês, não pt-BR."
            textSize = 16f
        }, matchParent())

        input = EditText(this).apply {
            hint = "Texto em inglês ou chinês"
            setText("This is a local Kokoro test for Tetirua.")
            minLines = 4
            gravity = android.view.Gravity.TOP
        }
        root.addView(input, matchParent())

        loadButton = Button(this).apply {
            text = "Carregar Kokoro"
            setOnClickListener { loadRuntime() }
        }
        root.addView(loadButton, matchParent())

        speakButton = Button(this).apply {
            text = "Gerar e falar"
            isEnabled = false
            setOnClickListener { speak() }
        }
        root.addView(speakButton, matchParent())

        root.addView(Button(this).apply {
            text = "Parar áudio"
            setOnClickListener {
                engine.stop()
                statusText.text = "Estado: reprodução parada"
            }
        }, matchParent())

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
        statusText.text = "Estado: carregando Kokoro + sherpa-onnx"
        Thread {
            runCatching { engine.load() }
                .onSuccess {
                    runOnUiThread {
                        loadButton.isEnabled = true
                        speakButton.isEnabled = true
                        statusText.text = "Estado: pronto para inglês/chinês"
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        loadButton.isEnabled = true
                        statusText.text = "Estado: prepare bibliotecas/modelo — ${error.message}"
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
        statusText.text = "Estado: gerando WAV Kokoro"
        engine.speak(
            SynthesisRequest(
                ids = AudioIds(),
                text = text,
                language = "en-US",
                outputMode = TtsOutputMode.PLAYBACK,
            ),
        ) { result ->
            runOnUiThread {
                speakButton.isEnabled = true
                resultText.text = buildString {
                    append("Engine: ${result.engine}\n")
                    append("Modelo: ${result.model}\n")
                    append("Voz: ${result.voice}\n")
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
