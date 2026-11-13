package soares.lucas.joao.com.example.gamehunter

import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import com.google.firebase.storage.FirebaseStorage

private const val TAMANHO_MAXIMO_DOWNLOAD = 10L * 1024 * 1024
private const val TAMANHO_ALVO_IMAGEM = 512

// Monta o caminho privado e previsível da foto de cada usuário.
fun caminhoFotoPerfil(uid: String): String = "usuarios/$uid/foto_perfil"

// Baixa e reduz a foto antes de exibi-la no ImageView existente.
fun carregarFotoPerfil(imageView: ImageView, caminho: String) {
    if (caminho.isBlank()) {
        return
    }

    FirebaseStorage.getInstance().reference.child(caminho)
        .getBytes(TAMANHO_MAXIMO_DOWNLOAD)
        .addOnSuccessListener { bytes ->
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)

            val opcoes = BitmapFactory.Options().apply {
                inSampleSize = calcularReducao(limites.outWidth, limites.outHeight)
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opcoes)

            if (bitmap != null) {
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setImageBitmap(bitmap)
                imageView.visibility = View.VISIBLE
            }
        }
}

// Calcula uma redução em potências de dois para evitar imagens grandes na memória.
private fun calcularReducao(largura: Int, altura: Int): Int {
    var reducao = 1
    while (largura / reducao > TAMANHO_ALVO_IMAGEM * 2 ||
        altura / reducao > TAMANHO_ALVO_IMAGEM * 2
    ) {
        reducao *= 2
    }
    return reducao
}
