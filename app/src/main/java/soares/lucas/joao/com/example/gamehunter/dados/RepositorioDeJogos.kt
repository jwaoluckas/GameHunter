package soares.lucas.joao.com.example.gamehunter.dados

import soares.lucas.joao.com.example.gamehunter.modelo.Genero
import soares.lucas.joao.com.example.gamehunter.modelo.Fileira
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// CONTRATO entre o Frontend e o Backend/Database: tudo o que as telas precisam pedir
// sobre jogos passa por aqui. As telas só conhecem esta interface, nunca a origem dos
// dados, então a implementação de exemplo pode ser trocada pela real (APIs, Room,
// Firebase...) sem mexer em nenhuma tela.
// As funções são "suspend" porque a versão real vai acessar a internet: elas rodam
// dentro de uma corrotina (lifecycleScope.launch) e não travam a tela (RNF04).
// Se algo der errado (sem internet, erro da API...), a implementação pode lançar uma
// exceção; o tratamento visual desses casos (RF10) ainda será feito nas telas
interface RepositorioDeJogos {

    // Média das ofertas na moeda solicitada. Null indica ausência de ofertas.
    // O catálogo de exemplo não simula preços regionais.
    suspend fun precoRegional(idItad: String, moeda: Moeda): Double? = null

    // Fileiras da tela Principal ("Mais vendidos", "De acordo com o que você gosta"...),
    // cada uma com título, subtítulo e a lista de jogos do carrossel
    suspend fun fileiras(generos: Set<Genero> = emptySet()): List<Fileira>

    // Busca por título (RF02), usada pela tela Resultado de pesquisas.
    // Implementação atual: pesquisa, informações e preços da IsThereAnyDeal.
    suspend fun buscarPorTitulo(texto: String): List<Jogo>

    // Todos os jogos de uma fileira, usado pelo botão "Ver tudo" (abre o Resultado
    // de pesquisas). Recebe o mesmo título que veio em fileiras()
    suspend fun jogosDaFileira(tituloDaFileira: String, generos: Set<Genero> = emptySet()): List<Jogo>
}
