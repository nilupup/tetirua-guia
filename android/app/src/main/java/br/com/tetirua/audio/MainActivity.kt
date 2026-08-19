package br.com.tetirua.audio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.tetirua.audio.core.AudioIds
import br.com.tetirua.audio.stt.MoonshineSttEngine
import java.util.concurrent.Executors

/** Aplicação de demonstração somente do runtime Moonshine Voice Android. */
class MainActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var engine: MoonshineSttEngine
    private lateinit var statusText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var loadButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = MoonshineSttEngine(this, language = "en")
        setContentView(createContent())
        requestAudioPermissionIfNeeded()
        loadModel()
    }

    private fun createContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        root.addView(TextView(this).apply {
            text = "Tetiruã — Moonshine Voice"
            textSize = 24f
        }, matchParent())
        root.addView(TextView(this).apply {
            text = "Branch independente de STT. O artefato oficial baixa o modelo no primeiro uso e mantém a transcrição no dispositivo. Demonstração configurada para inglês porque o catálogo consultado não publica pt-BR."
            textSize = 16f
        }, matchParent())

        loadButton = Button(this).apply {
            text = "Carregar modelo"
            setOnClickListener { loadModel() }
        }
        root.addView(loadButton, matchParent())

        startButton = Button(this).apply {
            text = "Iniciar microfone"
            isEnabled = false
            setOnClickListener { startListening() }
        }
        root.addView(startButton, matchParent())

        stopButton = Button(this).apply {
            text = "Parar e finalizar"
            isEnabled = false
            setOnClickListener { stopListening() }
        }
        root.addView(stopButton, matchParent())

        transcriptText = TextView(this).apply {
            text = "Transcrição: —"
            textSize = 18f
            setPadding(0, 24, 0, 24)
        }
        root.addView(transcriptText, matchParent())

        statusText = TextView(this).apply {
            text = "Estado: iniciando"
            textSize = 15f
        }
        root.addView(statusText, matchParent())

        return ScrollView(this).apply { addView(root) }
    }

    private fun loadModel() {
        loadButton.isEnabled = false
        startButton.isEnabled = false
        statusText.text = "Estado: carregando modelo Moonshine (primeiro uso pode baixar arquivos)"
        worker.execute {
            runCatching {
                engine.load { fraction, file ->
                    runOnUiThread {
                        statusText.text = "Estado: baixando $file (${(fraction * 100).toInt()}%)"
                    }
                }
            }.onSuccess {
                runOnUiThread {
                    loadButton.isEnabled = true
                    startButton.isEnabled = true
                    statusText.text = "Estado: pronto; modelo Moonshine carregado"
                }
            }.onFailure { error ->
                runOnUiThread {
                    loadButton.isEnabled = true
                    statusText.text = "Estado: falha ao carregar — ${error.message}"
                }
            }
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermissionIfNeeded()
            statusText.text = "Estado: conceda permissão de microfone e tente novamente"
            return
        }
        if (listening) return

        val ids = AudioIds()
        worker.execute {
            runCatching {
                engine.startListening(
                    ids = ids,
                    onPartial = { text ->
                        runOnUiThread { transcriptText.text = "Transcrição parcial:\n$text" }
                    },
                    onStatus = { status -> runOnUiThread { statusText.text = status } },
                )
            }.onSuccess {
                runOnUiThread {
                    listening = true
                    startButton.isEnabled = false
                    stopButton.isEnabled = true
                    statusText.text = "Estado: ouvindo — fale em inglês"
                }
            }.onFailure { error ->
                runOnUiThread { statusText.text = "Estado: falha ao iniciar — ${error.message}" }
            }
        }
    }

    private fun stopListening() {
        if (!listening) return
        worker.execute {
            val result = runCatching { engine.stopListening() }
            runOnUiThread {
                listening = false
                startButton.isEnabled = true
                stopButton.isEnabled = false
                result.onSuccess { transcription ->
                    transcriptText.text = "Transcrição final:\n${transcription.text.ifBlank { "(nenhum texto reconhecido)" }}"
                    statusText.text = "Estado: concluído — ${transcription.engine}"
                }.onFailure { error ->
                    statusText.text = "Estado: falha ao finalizar — ${error.message}"
                }
            }
        }
    }

    private fun requestAudioPermissionIfNeeded() {
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

    override fun onDestroy() {
        if (listening) runCatching { engine.stopListening() }
        engine.close()
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun matchParent() = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 2001
    }
}
