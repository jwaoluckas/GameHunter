package soares.lucas.joao.com.example.gamehunter.modelo

// Filtros escolhidos no modal "Filtros". Conjuntos vazios significam "todas as
// plataformas/lojas" e precoMaximo null significa "qualquer valor".
// Os valores padrão representam "nenhum filtro"
data class Filtros(
    val ordenacao: Ordenacao = Ordenacao.RELEVANCIA,
    val plataformas: Set<Plataforma> = emptySet(),
    val lojas: Set<Loja> = emptySet(),
    val precoMaximo: Int? = null,
    val somentePromocao: Boolean = false
) {

    // Opções de preço máximo (em reais) exibidas como chips no modal
    companion object {
        val PRECOS_MAXIMOS = listOf(50, 100, 200)
    }

    // Mantém só os jogos que passam em todos os filtros e depois os ordena.
    // Em "Relevância" a ordem original da lista é mantida
    fun aplicar(jogos: List<Jogo>): List<Jogo> {
        val filtrados = jogos.filter { jogo ->
            val plataformaOk = plataformas.isEmpty() || jogo.plataformas.any { it in plataformas }
            val lojaOk = lojas.isEmpty() || jogo.ofertas.any { it.loja in lojas }
            val precoOk = precoMaximo == null || jogo.precoMedioHoje <= precoMaximo
            val promocaoOk = !somentePromocao || jogo.emPromocao
            plataformaOk && lojaOk && precoOk && promocaoOk
        }

        return when (ordenacao) {
            Ordenacao.RELEVANCIA -> filtrados
            Ordenacao.MENOR_PRECO -> filtrados.sortedBy { it.precoMedioHoje }
            Ordenacao.MAIOR_DESCONTO -> filtrados.sortedByDescending { it.desconto }
            Ordenacao.MELHOR_AVALIACAO -> filtrados.sortedByDescending { it.nota }
        }
    }
}
