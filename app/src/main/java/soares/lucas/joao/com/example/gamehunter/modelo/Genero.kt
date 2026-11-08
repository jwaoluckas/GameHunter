package soares.lucas.joao.com.example.gamehunter.modelo

import soares.lucas.joao.com.example.gamehunter.R

// Texto em português na interface; tag original nas consultas à IsThereAnyDeal.
enum class Genero(val texto: Int, val tag: String) {
    ACAO(R.string.genero_acao, "Action"),
    AVENTURA(R.string.genero_aventura, "Adventure"),
    RPG(R.string.genero_rpg, "RPG"),
    ESTRATEGIA(R.string.genero_estrategia, "Strategy"),
    SIMULACAO(R.string.genero_simulacao, "Simulation"),
    CORRIDA(R.string.genero_corrida, "Racing"),
    ESPORTES(R.string.genero_esportes, "Sports"),
    TERROR(R.string.genero_terror, "Horror");

    fun corresponde(tags: Set<String>): Boolean = tags.any { it.equals(tag, ignoreCase = true) }
}
