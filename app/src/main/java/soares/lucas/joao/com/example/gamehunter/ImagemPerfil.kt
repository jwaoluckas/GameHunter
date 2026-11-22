package soares.lucas.joao.com.example.gamehunter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private const val TAMANHO_ALVO_IMAGEM = 512
private const val QUALIDADE_JPEG = 85

// Copia a foto escolhida para a pasta privada do app, separada pelo UID da conta.
// O arquivo e reduzido antes de ser salvo para ocupar pouco espaco no dispositivo.
fun salvarFotoPerfilLocal(contexto: Context, uid: String, origem: Uri): String? = runCatching {
    val bitmap = decodificarFoto(contexto, origem) ?: return null
    val pasta = File(contexto.filesDir, "perfis/$uid").apply { mkdirs() }
    val destino = File(pasta, "foto_perfil.jpg")
    val temporario = File(pasta, "foto_perfil.tmp")

    FileOutputStream(temporario).use { saida ->
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida))
    }
    if (destino.exists()) destino.delete()
    if (!temporario.renameTo(destino)) {
        temporario.copyTo(destino, overwrite = true)
        temporario.delete()
    }
    destino.absolutePath
}.getOrNull()

// Remove a copia local quando um cadastro nao consegue ser concluido.
fun removerFotoPerfilLocal(caminho: String) {
    if (caminho.isNotBlank()) runCatching { File(caminho).delete() }
}

// Carrega somente o arquivo privado local; nenhuma consulta ao Firebase Storage e feita.
fun carregarFotoPerfil(imageView: ImageView, caminho: String) {
    val arquivo = caminho.takeIf { it.isNotBlank() }?.let(::File) ?: return
    if (!arquivo.isFile) return

    val bitmap = BitmapFactory.decodeFile(arquivo.absolutePath) ?: return
    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
    imageView.setImageBitmap(bitmap)
    imageView.visibility = View.VISIBLE
}

private fun decodificarFoto(contexto: Context, origem: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val fonte = ImageDecoder.createSource(contexto.contentResolver, origem)
        return ImageDecoder.decodeBitmap(fonte) { decoder, info, _ ->
            val largura = info.size.width
            val altura = info.size.height
            val escala = minOf(
                TAMANHO_ALVO_IMAGEM.toFloat() / largura,
                TAMANHO_ALVO_IMAGEM.toFloat() / altura,
                1f
            )
            decoder.setTargetSize(
                (largura * escala).roundToInt().coerceAtLeast(1),
                (altura * escala).roundToInt().coerceAtLeast(1)
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contexto.contentResolver.openInputStream(origem)?.use {
        BitmapFactory.decodeStream(it, null, limites)
    }
    if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

    val opcoes = BitmapFactory.Options().apply {
        inSampleSize = calcularReducao(limites.outWidth, limites.outHeight)
    }
    val original = contexto.contentResolver.openInputStream(origem)?.use {
        BitmapFactory.decodeStream(it, null, opcoes)
    } ?: return null

    val escala = minOf(
        TAMANHO_ALVO_IMAGEM.toFloat() / original.width,
        TAMANHO_ALVO_IMAGEM.toFloat() / original.height,
        1f
    )
    if (escala >= 1f) return original
    return Bitmap.createScaledBitmap(
        original,
        (original.width * escala).roundToInt().coerceAtLeast(1),
        (original.height * escala).roundToInt().coerceAtLeast(1),
        true
    ).also { reduzido ->
        if (reduzido !== original) original.recycle()
    }
}

private fun calcularReducao(largura: Int, altura: Int): Int {
    var reducao = 1
    while (largura / reducao > TAMANHO_ALVO_IMAGEM * 2 ||
        altura / reducao > TAMANHO_ALVO_IMAGEM * 2
    ) {
        reducao *= 2
    }
    return reducao
}
