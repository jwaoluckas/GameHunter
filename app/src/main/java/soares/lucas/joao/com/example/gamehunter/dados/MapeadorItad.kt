package soares.lucas.joao.com.example.gamehunter.dados

import org.json.JSONArray
import org.json.JSONObject
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Loja
import soares.lucas.joao.com.example.gamehunter.modelo.Oferta
import soares.lucas.joao.com.example.gamehunter.modelo.Plataforma

internal fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.texto(chave: String): String = if (isNull(chave)) "" else optString(chave)

// Os valores ausentes não viram zeros nem são preenchidos com o catálogo fictício.
internal object MapeadorItad {
    fun jogo(base: JSONObject, detalhes: JSONObject, precos: JSONObject?): Jogo {
        val id = base.getString("id")
        val ofertas = precos?.optJSONArray("deals")?.objetos().orEmpty()
            .mapNotNull(::oferta).sortedBy { it.preco }
        val reviews = detalhes.optJSONArray("reviews")?.objetos().orEmpty()
        val review = reviews.firstOrNull { it.texto("source") == "Metascore" }
            ?: reviews.firstOrNull { it.texto("source") == "Steam" }
        val nota = review?.optDouble("score", Double.NaN)?.div(10) ?: Double.NaN
        val tags = detalhes.optJSONArray("tags")
        val assets = detalhes.optJSONObject("assets") ?: base.optJSONObject("assets")
        val precoOriginal = ofertas.firstOrNull()?.precoRegular
            ?.takeIf { it.isFinite() && it >= 0 } ?: Double.NaN
        return Jogo(
            id = id.hashCode(), // Identidade completa preservada em idItad.
            idItad = id,
            titulo = base.getString("title"),
            capaUrl = assets?.texto("boxart")?.takeIf { it.startsWith("https://") },
            nota = nota.takeIf { it in 0.0..10.0 } ?: Double.NaN,
            // A API informa o preço regular da oferta, usado no card como preço original.
            precoLancamento = precoOriginal,
            plataformas = listOf(Plataforma.PC),
            genero = tags?.optString(0).orEmpty(),
            desenvolvedora = detalhes.optJSONArray("developers")?.objetos().orEmpty()
                .map { it.texto("name") }.filter { it.isNotBlank() }.joinToString(", "),
            anoLancamento = detalhes.texto("releaseDate").take(4).toIntOrNull() ?: 0,
            resumo = "", // A API não fornece sinopse.
            precoEmDolar = Double.NaN,
            ofertas = ofertas,
            tags = if (tags == null) emptySet() else (0 until tags.length())
                .map { tags.optString(it) }.filter { it.isNotBlank() }.toSet()
        )
    }

    private fun oferta(json: JSONObject): Oferta? {
        val preco = json.optJSONObject("price") ?: return null
        // Mesmo com country=BR, algumas lojas podem retornar outra moeda.
        if (preco.texto("currency") != "BRL") return null
        val valor = preco.optDouble("amount", Double.NaN)
        if (!valor.isFinite() || valor < 0) return null
        // Cupons exigiriam apresentar instruções adicionais; solicitamos vouchers=false.
        if (json.texto("voucher").isNotBlank()) return null
        val shop = json.optJSONObject("shop") ?: return null
        val nome = shop.texto("name").takeIf { it.isNotBlank() } ?: return null
        val url = json.texto("url").takeIf { it.startsWith("https://") } ?: return null
        val loja = when (shop.optInt("id")) {
            61 -> Loja.STEAM
            16 -> Loja.EPIC_GAMES
            50 -> Loja.NUUVEM
            else -> null // Preserva o nome de lojas fora das opções fixas dos filtros.
        }
        val regular = json.optJSONObject("regular")
        val valorRegular = if (regular?.texto("currency") == "BRL") {
            regular.optDouble("amount", Double.NaN).takeIf { it.isFinite() && it >= 0 } ?: Double.NaN
        } else Double.NaN
        return Oferta(loja, valor, nome, "PC", url, valorRegular)
    }
}
