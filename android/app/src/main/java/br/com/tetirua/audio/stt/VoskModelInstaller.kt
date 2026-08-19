package br.com.tetirua.audio.stt

import android.content.Context
import java.io.File

/** Copia o modelo Vosk dos assets para um diretório local acessível ao runtime. */
object VoskModelInstaller {
    const val MODEL_ASSET_DIR = "vosk-model-small-pt-0.3"

    fun installIfNeeded(context: Context): File {
        val targetDir = File(context.filesDir, MODEL_ASSET_DIR)
        if (File(targetDir, "final.mdl").isFile && File(targetDir, "HCLr.fst").isFile) {
            return targetDir
        }

        val tempDir = File(context.filesDir, "$MODEL_ASSET_DIR.tmp")
        tempDir.deleteRecursively()
        copyAssetTree(context, MODEL_ASSET_DIR, tempDir)

        check(File(tempDir, "final.mdl").isFile) {
            "Modelo Vosk inválido: final.mdl não foi encontrado em $MODEL_ASSET_DIR"
        }
        check(File(tempDir, "HCLr.fst").isFile) {
            "Modelo Vosk inválido: HCLr.fst não foi encontrado em $MODEL_ASSET_DIR"
        }

        targetDir.deleteRecursively()
        check(tempDir.renameTo(targetDir)) {
            "Não foi possível instalar o modelo Vosk em ${targetDir.absolutePath}"
        }
        return targetDir
    }

    private fun copyAssetTree(context: Context, assetPath: String, target: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }

        target.mkdirs()
        for (child in children) {
            copyAssetTree(context, "$assetPath/$child", File(target, child))
        }
    }
}
