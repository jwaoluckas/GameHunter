package soares.lucas.joao.com.example.gamehunter.modelo

import soares.lucas.joao.com.example.gamehunter.R

enum class SelecaoConteudo(val texto: Int, val tiposItad: List<Int>) {
    JOGOS(R.string.filtros_jogos, listOf(1)),
    DLC(R.string.filtros_dlc, listOf(2)),
    TODOS(R.string.filtros_todos_conteudos, listOf(1, 2));

    fun aceita(tipo: TipoConteudo): Boolean = when (this) {
        JOGOS -> tipo == TipoConteudo.JOGO
        DLC -> tipo == TipoConteudo.DLC
        TODOS -> tipo == TipoConteudo.JOGO || tipo == TipoConteudo.DLC
    }
}
