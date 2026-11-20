package soares.lucas.joao.com.example.gamehunter.dados

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

internal data class PromocaoAtual(
    val idItad: String,
    val precoMedio: Double,
    val desconto: Int
) {
    val emPromocao: Boolean
        get() = desconto > 0
}

internal object LeitorPromocoesItad {
    fun ler(resposta: String): Map<String, PromocaoAtual> = JSONArray(resposta)
        .objetos()
        .mapNotNull(::lerJogo)
        .associateBy { it.idItad }

    private fun lerJogo(jogo: JSONObject): PromocaoAtual? {
        val id = jogo.optString("id").takeIf { it.isNotBlank() } ?: return null
        val ofertas = jogo.optJSONArray("deals")?.objetos().orEmpty().mapNotNull(::lerOferta)
        val comparaveis = ofertas.filter { it.precoRegular.isFinite() && it.precoRegular > 0 }
        val desconto = if (comparaveis.isEmpty()) {
            0
        } else {
            ((1 - comparaveis.sumOf { it.preco } / comparaveis.sumOf { it.precoRegular }) * 100)
                .roundToInt().coerceIn(0, 100)
        }
        return PromocaoAtual(
            idItad = id,
            precoMedio = ofertas.map { it.preco }.takeIf { it.isNotEmpty() }?.average() ?: Double.NaN,
            desconto = desconto
        )
    }

    private fun lerOferta(json: JSONObject): OfertaAtual? {
        if (!json.isNull("voucher") && json.optString("voucher").isNotBlank()) return null
        val preco = json.optJSONObject("price") ?: return null
        if (preco.optString("currency") != "BRL") return null
        val valor = preco.optDouble("amount", Double.NaN)
        if (!valor.isFinite() || valor < 0) return null

        val regular = json.optJSONObject("regular")
        val valorRegular = if (regular?.optString("currency") == "BRL") {
            regular.optDouble("amount", Double.NaN).takeIf { it.isFinite() && it >= 0 } ?: Double.NaN
        } else {
            Double.NaN
        }
        return OfertaAtual(valor, valorRegular)
    }

    private data class OfertaAtual(val preco: Double, val precoRegular: Double)
}
