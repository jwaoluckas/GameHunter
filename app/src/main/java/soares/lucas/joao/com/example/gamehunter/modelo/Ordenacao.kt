package soares.lucas.joao.com.example.gamehunter.modelo

import androidx.annotation.StringRes
import soares.lucas.joao.com.example.gamehunter.R

// Formas de ordenar os jogos no modal de filtros. "texto" é o id do texto
// exibido no chip (fica no strings.xml)
enum class Ordenacao(@StringRes val texto: Int) {
    RELEVANCIA(R.string.ordenar_relevancia),
    MENOR_PRECO(R.string.ordenar_menor_preco),
    MAIOR_DESCONTO(R.string.ordenar_maior_desconto),
    MELHOR_AVALIACAO(R.string.ordenar_melhor_avaliacao)
}
