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
import br.com.tetirua.audio.tts.AndroidTtsEngine
import java.io.File

/** Aplicação demonstrativa somente da API nativa Android TextToSpeech. */
class MainActivity : AppCompatActivity() {
    private lateinit var engine: AndroidTtsEngine
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var input: EditText
    private lateinit var speakButton: Button
    private lateinit var fileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = AndroidTtsEngine(this) { ready ->
            runOnUiThread {
                if (::statusText.isInitialized) {
                    speakButton.isEnabled = ready
                    fileButton.isEnabled = ready
                    statusText.text = if (ready) {
                        "Estado: Android TTS pronto; voz pt-BR solicitada"
                    } else {
                        "Estado: mecanismo Android TTS indisponível"
                    }
                }
            }
        }
        setContentView(createContent())
    }

    private fun createContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        root.addView(TextView(this).apply {
            text = "Tetiruã — Android TTS nativo"
            textSize = 24f
        }, matchParent())
        root.addView(TextView(this).apply {
            text = "Branch independente sem pesos ou bibliotecas externas. Usa android.speech.tts.TextToSpeech, mostra o resultado na tela e pode gerar um WAV no cache."
            textSize = 16f
        }, matchParent())

        input = EditText(this).apply {
            hint = "Texto em português brasileiro"
            setText("Olá. Este é um teste do Tetiruã usando o mecanismo nativo do Android.")
            minLines = 4
            gravity = android.view.Gravity.TOP
        }
        root.addView(input, matchParent())

        speakButton = Button(this).apply {
            text = "Falar resposta"
            isEnabled = false
            setOnClickListener { speak(playback = true) }
        }
        root.addView(speakButton, matchParent())

        fileButton = Button(this).apply {
            text = "Gerar WAV no cache"
            isEnabled = false
            setOnClickListener { speak(playback = false) }
        }
        root.addView(fileButton, matchParent())

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
            text = "Estado: inicializando mecanismo Android TTS"
            textSize = 15f
        }
        root.addView(statusText, matchParent())

        return ScrollView(this).apply { addView(root) }
    }

    private fun speak(playback: Boolean) {
        val text = input.text.toString().trim()
        if (text.isEmpty()) {
            statusText.text = "Estado: informe um texto"
            return
        }

        speakButton.isEnabled = false
        fileButton.isEnabled = false
        statusText.text = if (playback) "Estado: falando" else "Estado: sintetizando WAV"
        val output = File(cacheDir, "tetirua-native-${System.currentTimeMillis()}.wav")
        engine.speak(
            SynthesisRequest(
                ids = AudioIds(),
                text = text,
                language = "pt-BR",
                outputMode = if (playback) TtsOutputMode.PLAYBACK else TtsOutputMode.FILE,
                outputPath = if (playback) null else output.absolutePath,
            ),
        ) { result ->
            runOnUiThread {
                speakButton.isEnabled = engine.isReady()
                fileButton.isEnabled = engine.isReady()
                resultText.text = buildString {
                    append("Engine: ${result.engine}\n")
                    append("Idioma: pt-BR\n")
                    append("Arquivo: ${result.audioPath ?: "não informado"}\n")
                    append("Duração/tempo: ${result.durationMs ?: 0} ms")
                    if (result.warnings.isNotEmpty()) {
                        append("\nAviso: ${result.warnings.joinToString()}")
                    }
                }
                statusText.text = if (result.warnings.isEmpty()) {
                    "Estado: concluído"
                } else {
                    "Estado: concluído com aviso"
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
