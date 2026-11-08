package soares.lucas.joao.com.example.gamehunter.modelo

// A API consulta países, não converte moedas. A moeda retornada precisa coincidir
// com a solicitada; disponibilidade depende da região e da loja.
enum class Moeda(
    val nome: String,
    val regiao: String,
    val regiaoCurta: String,
    val simbolo: String,
    val pais: String
) {
    USD("Dólar", "Estados Unidos", "EUA", "US$", "US"),
    BRL("Real", "Brasil", "Brasil", "R$", "BR"),
    EUR("Euro", "Alemanha", "Alemanha", "€", "DE"),
    GBP("Libra", "Reino Unido", "Reino Unido", "£", "GB"),
    JPY("Iene", "Japão", "Japão", "¥", "JP"),
    ARS("Peso argentino", "Argentina", "Argentina", "AR$", "AR"),
    MXN("Peso mexicano", "México", "México", "MX$", "MX"),
    CAD("Dólar canadense", "Canadá", "Canadá", "CA$", "CA")
}
