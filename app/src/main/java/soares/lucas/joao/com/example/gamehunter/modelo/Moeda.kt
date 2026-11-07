package soares.lucas.joao.com.example.gamehunter.modelo

// Moedas disponíveis no modal "Comparar moedas" e nas Configurações.
// - cotacaoEmReais: quantos reais vale 1 unidade da moeda
// - fatorPrecoRegional: quanto custa na região, em moeda local, um jogo que custa US$ 1
//   nos EUA (simula o "preço de tabela" de cada região; não é usado no BRL)
// Os valores são apenas de EXEMPLO.
// TODO (API): buscar as cotações na API de câmbio
enum class Moeda(
    val nome: String,
    val regiao: String,
    val regiaoCurta: String,
    val simbolo: String,
    val cotacaoEmReais: Double,
    val fatorPrecoRegional: Double
) {
    USD("Dólar", "Estados Unidos", "EUA", "US$", 5.50, 1.0),
    BRL("Real", "Brasil", "Brasil", "R$", 1.0, 1.0),
    EUR("Euro", "Europa", "Europa", "€", 6.40, 0.85712),
    GBP("Libra", "Reino Unido", "Reino Unido", "£", 7.30, 0.786),
    JPY("Iene", "Japão", "Japão", "¥", 0.037, 150.0),
    ARS("Peso argentino", "Argentina", "Argentina", "AR$", 0.0055, 900.0),
    MXN("Peso mexicano", "México", "México", "MX$", 0.30, 17.0),
    CAD("Dólar canadense", "Canadá", "Canadá", "CA$", 4.00, 1.40);

    // Preço do jogo nesta moeda: no Brasil é a média das lojas (em reais);
    // nas outras regiões é o preço de tabela, calculado a partir do preço nos EUA
    fun precoDoJogo(jogo: Jogo): Double {
        return if (this == BRL) jogo.precoMedioHoje else jogo.precoEmDolar * fatorPrecoRegional
    }

    // Converte um valor desta moeda para a moeda de destino, passando pelo real
    fun converter(valor: Double, destino: Moeda): Double {
        return valor * cotacaoEmReais / destino.cotacaoEmReais
    }
}
