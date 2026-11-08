package soares.lucas.joao.com.example.gamehunter.modelo

// Filtros escolhidos no modal "Filtros". Conjuntos vazios significam "todas as
// plataformas/lojas" e precoMaximo null significa "qualquer valor".
// Os valores padrão representam "nenhum filtro"
data class Filtros(
    val ordenacao: Ordenacao = Ordenacao.RELEVANCIA,
    val generos: Set<Genero> = emptySet(),
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
            val generoOk = generos.isEmpty() || generos.any { it.corresponde(jogo.tags) }
            val lojaOk = lojas.isEmpty() || jogo.ofertas.any { it.loja in lojas }
            val precoOk = precoMaximo == null || jogo.precoMedioHoje <= precoMaximo
            val promocaoOk = !somentePromocao || jogo.emPromocao
            generoOk && lojaOk && precoOk && promocaoOk
        }

        return when (ordenacao) {
            Ordenacao.RELEVANCIA -> filtrados
            Ordenacao.MENOR_PRECO -> filtrados.sortedBy { it.precoMedioHoje }
            Ordenacao.MAIOR_DESCONTO -> filtrados.sortedByDescending { it.desconto }
            Ordenacao.MELHOR_AVALIACAO -> filtrados.sortedByDescending { it.nota.takeIf { nota -> nota.isFinite() } ?: -1.0 }
        }
    }
}
