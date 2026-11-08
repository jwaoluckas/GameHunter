package soares.lucas.joao.com.example.gamehunter.modelo

// Uma oferta de um jogo em uma loja, com o preço atual em reais
data class Oferta(
    val loja: Loja?,
    val preco: Double,
    val nomeLoja: String = loja?.nome.orEmpty(),
    val plataforma: String = loja?.plataforma ?: "PC",
    val url: String? = null,
    val precoRegular: Double = Double.NaN
)
