package soares.lucas.joao.com.example.gamehunter.modelo

// Uma fileira (seção) da tela Principal, como "Mais vendidos", com os jogos do seu carrossel
data class Fileira(
    val titulo: String,
    val subtitulo: String,
    val jogos: List<Jogo>
)
