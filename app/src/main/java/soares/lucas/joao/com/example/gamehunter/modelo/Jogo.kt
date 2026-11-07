package soares.lucas.joao.com.example.gamehunter.modelo

import kotlin.math.roundToInt

// Um jogo, com as informações exibidas nos cards e no modal de detalhes.
// Preços em reais (exceto precoEmDolar, o preço de tabela nos EUA) e nota de 0 a 10.
// capaUrl pode ser null enquanto a API não estiver integrada
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
    val ofertas: List<Oferta>
) {

    // Valores calculados a partir das ofertas:
    // - precoMedioHoje: média dos preços das lojas (0 se não houver ofertas)
    // - desconto: quanto a média de hoje está abaixo do preço de lançamento, em %
    // - emPromocao: true quando existe algum desconto
    // - ofertasPorPreco: ofertas da mais barata para a mais cara
    val precoMedioHoje: Double
        get() = if (ofertas.isEmpty()) 0.0 else ofertas.map { it.preco }.average()

    val desconto: Int
        get() {
            if (precoLancamento <= 0.0) return 0
            val percentual = (1 - precoMedioHoje / precoLancamento) * 100
            return percentual.roundToInt().coerceAtLeast(0)
        }

    val emPromocao: Boolean
        get() = desconto > 0

    val ofertasPorPreco: List<Oferta>
        get() = ofertas.sortedBy { it.preco }
}
