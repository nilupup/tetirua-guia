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
import br.com.tetirua.audio.stt.WhisperAudioRecorder
import br.com.tetirua.audio.stt.WhisperCppSttEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

/** Aplicação desta branch: somente whisper.cpp para STT Android. */
class MainActivity : AppCompatActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recorder = WhisperAudioRecorder()
    private var whisperEngine: WhisperCppSttEngine? = null
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
            text = "Tetiruã — Whisper.cpp STT"
            textSize = 24f
        }, matchParent())

        root.addView(TextView(this).apply {
            text = "Esta branch executa somente whisper.cpp. O modelo esperado é models/ggml-base.bin, em português multilíngue."
            textSize = 16f
        }, matchParent())

        val recordButton = Button(this).apply {
            text = "Gravar 6 segundos e transcrever"
            setOnClickListener { recordAndTranscribe() }
        }
        root.addView(recordButton, matchParent())

        val releaseButton = Button(this).apply {
            text = "Liberar modelo"
            setOnClickListener {
                whisperEngine?.release()
                whisperEngine = null
                statusText.text = "Estado: modelo liberado"
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

        activityScope.launch {
            val output = File(cacheDir, "tetirua-whisper-${System.currentTimeMillis()}.wav")
            try {
                statusText.text = "Estado: gravando microfone por 6 segundos"
                recorder.recordToWav(output)

                statusText.text = "Estado: carregando whisper.cpp e modelo"
                val engine = whisperEngine ?: WhisperCppSttEngine(this@MainActivity).also {
                    whisperEngine = it
                }

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
                resultText.text = "Verifique se o modelo existe em app/src/main/assets/models/ggml-base.bin."
            } finally {
                output.delete()
            }
        }
    }

    private fun requestAudioPermissionIfNeeded() {
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION,
            )
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        whisperEngine?.release()
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
