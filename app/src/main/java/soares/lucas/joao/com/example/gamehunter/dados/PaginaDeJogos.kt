package soares.lucas.joao.com.example.gamehunter.dados

import soares.lucas.joao.com.example.gamehunter.modelo.Jogo

data class PaginaDeJogos(
    val jogos: List<Jogo>,
    val temMais: Boolean
)
