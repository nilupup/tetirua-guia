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
import br.com.tetirua.audio.core.TranscriptionRequest
import br.com.tetirua.audio.stt.VoskAudioRecorder
import br.com.tetirua.audio.stt.VoskSttEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

/** Aplicação demonstrativa desta branch: somente Vosk/Kaldi para STT Android. */
class MainActivity : AppCompatActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recorder = VoskAudioRecorder()
    private var voskEngine: VoskSttEngine? = null
    private lateinit var recordButton: Button
    private lateinit var releaseButton: Button
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContent())
        requestAudioPermissionIfNeeded()
    }

    private fun createContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        root.addView(TextView(this).apply {
            text = "Tetiruã — Vosk/Kaldi STT"
            textSize = 24f
        }, matchParent())

        root.addView(TextView(this).apply {
            text = "Branch independente: Vosk/Kaldi + AAR Android. Modelo: vosk-model-small-pt-0.3. Processamento offline."
            textSize = 16f
        }, matchParent())

        recordButton = Button(this).apply {
            text = "Gravar 6 segundos e transcrever"
            setOnClickListener { recordAndTranscribe() }
        }
        root.addView(recordButton, matchParent())

        releaseButton = Button(this).apply {
            text = "Liberar modelo"
            isEnabled = false
            setOnClickListener {
                voskEngine?.release()
                voskEngine = null
                statusText.text = "Estado: modelo liberado; será carregado no próximo teste"
                releaseButton.isEnabled = false
            }
        }
        root.addView(releaseButton, matchParent())

        statusText = TextView(this).apply {
            text = "Estado: aguardando microfone"
            textSize = 15f
        }
        root.addView(statusText, matchParent())

        resultText = TextView(this).apply {
            text = "Transcrição: —"
            textSize = 18f
            setPadding(0, 24, 0, 0)
        }
        root.addView(resultText, matchParent())

        return ScrollView(this).apply { addView(root) }
    }

    private fun recordAndTranscribe() {
        if (!hasAudioPermission()) {
            statusText.text = "Estado: permissão de microfone necessária"
            requestAudioPermissionIfNeeded()
            return
        }

        setBusy(true)
        activityScope.launch {
            val output = File(cacheDir, "tetirua-vosk-${System.currentTimeMillis()}.wav")
            try {
                statusText.text = "Estado: gravando microfone por 6 segundos"
                recorder.recordToWav(output)

                statusText.text = "Estado: carregando modelo Vosk local"
                val engine = voskEngine ?: VoskSttEngine(this@MainActivity).also {
                    voskEngine = it
                }
                statusText.text = "Estado: modelo selecionado — ${engine.modelName}"

                statusText.text = "Estado: transcrevendo localmente, sem internet"
                val result = engine.transcribe(
                    TranscriptionRequest(
                        ids = AudioIds(),
                        audioPath = output.absolutePath,
                        languageHint = "pt-BR",
                        streaming = false,
                    ),
                )
                resultText.text = "Transcrição (${result.engine}/${result.model}):\n${result.text}"
                statusText.text = "Estado: concluído em ${result.processingTimeMs ?: 0} ms"
            } catch (error: Exception) {
                statusText.text = "Estado: erro — ${error.message ?: error.javaClass.simpleName}"
                resultText.text = "Diagnóstico: confira a permissão, o modelo Vosk nos assets e a dependência do AAR."
            } finally {
                output.delete()
                setBusy(false)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        recordButton.isEnabled = !busy
        releaseButton.isEnabled = !busy && voskEngine != null
    }

    private fun requestAudioPermissionIfNeeded() {
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION,
            )
        } else {
            statusText.text = "Estado: pronto; modelo será carregado no primeiro teste"
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            statusText.text = if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                "Estado: pronto; modelo será carregado no primeiro teste"
            } else {
                "Estado: permissão de microfone negada"
            }
        }
    }

    override fun onDestroy() {
        voskEngine?.release()
        activityScope.cancel()
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
