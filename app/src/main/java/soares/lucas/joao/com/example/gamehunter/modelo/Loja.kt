package soares.lucas.joao.com.example.gamehunter.modelo

import android.net.Uri

// Lojas onde os jogos são vendidos. "plataforma" é o texto curto exibido abaixo do
// nome da loja na lista "Onde comprar" (ex.: Steam -> PC)
enum class Loja(
    val nome: String,
    val plataforma: String,
    private val urlDeBusca: String,
    val idItad: Int? = null
) {
    STEAM("Steam", "PC", "https://store.steampowered.com/search/?term=", 61),
    EPIC_GAMES("Epic Games", "PC", "https://store.epicgames.com/pt-BR/browse?q=", 16),
    NUUVEM("Nuuvem", "PC", "https://www.nuuvem.com/br-pt/catalog/search/", 50),
    MICROSOFT_STORE("Microsoft Store", "PC", "https://www.xbox.com/pt-BR/search?q=", 48),
    GOG("GOG", "PC", "https://www.gog.com/en/games?query=", 35),
    HUMBLE_STORE("Humble Store", "PC", "https://www.humblebundle.com/store/search?search=", 37),
    // Mantidas apenas para ler os dados demonstrativos e favoritos antigos.
    PLAYSTATION_STORE("PlayStation Store", "PlayStation", "https://store.playstation.com/pt-br/search/"),
    NINTENDO_ESHOP("Nintendo eShop", "Nintendo", "https://www.nintendo.com/pt-br/search/#q=");

    companion object {
        val DISPONIVEIS_NO_ITAD = listOf(
            STEAM,
            EPIC_GAMES,
            NUUVEM,
            MICROSOFT_STORE,
            GOG,
            HUMBLE_STORE
        )
    }

    // Endereço da busca pelo jogo no site da loja. Uri.encode troca espaços e
    // caracteres especiais do título por códigos aceitos em um endereço web.
    // Usado somente pelos dados de exemplo; ofertas reais fornecem Oferta.url.
    fun linkDeBusca(tituloDoJogo: String): String = urlDeBusca + Uri.encode(tituloDoJogo)
}
