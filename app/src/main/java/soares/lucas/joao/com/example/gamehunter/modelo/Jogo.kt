package soares.lucas.joao.com.example.gamehunter.modelo

import kotlin.math.roundToInt

// Um jogo, com as informações exibidas nos cards e no modal de detalhes.
// Preços em reais (exceto precoEmDolar, o preço de tabela nos EUA) e nota de 0 a 10.
// Dados numéricos ausentes da API usam NaN e são exibidos como travessão.
// capaUrl pode ser null quando a fonte não oferece imagem.
data class Jogo(
    val id: Int,
    val titulo: String,
    val capaUrl: String?,
    val nota: Double,
    val precoLancamento: Double,
    val plataformas: List<Plataforma>,
    val genero: String,
    val desenvolvedora: String,
    val anoLancamento: Int,
    val resumo: String,
    val precoEmDolar: Double,
    val ofertas: List<Oferta>,
    val idItad: String? = null,
    val tags: Set<String> = emptySet()
) {

    // Valores calculados a partir das ofertas:
    // - precoMedioHoje: média dos preços das lojas
    // - desconto: quanto a média de hoje está abaixo dos preços regulares, em %
    // - emPromocao: true quando existe algum desconto
    // - ofertasPorPreco: ofertas da mais barata para a mais cara
    val precoMedioHoje: Double
        get() = if (ofertas.isEmpty()) Double.NaN else ofertas.map { it.preco }.average()

    val desconto: Int
        get() {
            if (idItad != null) {
                val comparaveis = ofertas.filter { it.precoRegular.isFinite() && it.precoRegular > 0 }
                if (comparaveis.isEmpty()) return 0
                return ((1 - comparaveis.sumOf { it.preco } /
                    comparaveis.sumOf { it.precoRegular }) * 100).roundToInt().coerceIn(0, 100)
            }
            if (precoLancamento <= 0.0) return 0
            val percentual = (1 - precoMedioHoje / precoLancamento) * 100
            return percentual.roundToInt().coerceAtLeast(0)
        }

    val emPromocao: Boolean
        get() = desconto > 0

    val ofertasPorPreco: List<Oferta>
        get() = ofertas.sortedBy { it.preco }
}
