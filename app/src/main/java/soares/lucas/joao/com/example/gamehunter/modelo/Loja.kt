package soares.lucas.joao.com.example.gamehunter.modelo

import android.net.Uri

// Lojas onde os jogos são vendidos. "plataforma" é o texto curto exibido abaixo do
// nome da loja na lista "Onde comprar" (ex.: Steam -> PC)
enum class Loja(val nome: String, val plataforma: String, private val urlDeBusca: String) {
    STEAM("Steam", "PC", "https://store.steampowered.com/search/?term="),
    EPIC_GAMES("Epic Games", "PC", "https://store.epicgames.com/pt-BR/browse?q="),
    NUUVEM("Nuuvem", "PC", "https://www.nuuvem.com/br-pt/catalog/search/"),
    PLAYSTATION_STORE("PlayStation Store", "PlayStation", "https://store.playstation.com/pt-br/search/"),
    MICROSOFT_STORE("Microsoft Store", "Xbox", "https://www.xbox.com/pt-BR/search?q="),
    NINTENDO_ESHOP("Nintendo eShop", "Nintendo", "https://www.nintendo.com/pt-br/search/#q=");

    // Endereço da busca pelo jogo no site da loja. Uri.encode troca espaços e
    // caracteres especiais do título por códigos aceitos em um endereço web.
    // TODO (API): trocar pelo link de redirecionamento da oferta na CheapShark (dealID)
    fun linkDeBusca(tituloDoJogo: String): String = urlDeBusca + Uri.encode(tituloDoJogo)
}
