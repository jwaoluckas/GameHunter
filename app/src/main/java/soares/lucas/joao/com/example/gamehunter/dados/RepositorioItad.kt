package soares.lucas.joao.com.example.gamehunter.dados

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import soares.lucas.joao.com.example.gamehunter.modelo.Genero
import soares.lucas.joao.com.example.gamehunter.modelo.Fileira
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda
import soares.lucas.joao.com.example.gamehunter.modelo.Ordenacao
import soares.lucas.joao.com.example.gamehunter.modelo.SelecaoConteudo
import java.net.URLEncoder

// Cache de cinco minutos, limitado a 20 consultas. A lista inicial preserva a ordem
// do ranking de popularidade do ITAD (colecionados + desejados pela comunidade).
internal class RepositorioItad(
    private val consultar: suspend (String, String?) -> String,
    private val tituloOfertas: String,
    private val tituloDescontos: String,
    private val subtitulo: String,
    private val agora: () -> Long = { System.nanoTime() / 1_000_000 }
) : RepositorioDeJogos {
    private companion object {
        const val TAMANHO_PAGINA_HOME = 20
        const val MINIMO_DESTAQUES_HOME = 6
        const val MAXIMO_PAGINAS_DESTAQUES = 3
    }

    private data class Entrada(val instante: Long, val jogos: List<Jogo>)
    private val cache = LinkedHashMap<String, Entrada>()
    private val mutex = Mutex()
    private val limite = Semaphore(3)
    private val precosRegionais = PrecosRegionaisItad(consultar, agora)

    override suspend fun precoRegional(idItad: String, moeda: Moeda): Double? =
        precosRegionais.consultarPreco(idItad, moeda)

    override suspend fun fileiras(
        generos: Set<Genero>,
        ordenacao: Ordenacao,
        selecaoConteudo: SelecaoConteudo
    ): List<Fileira> {
        val populares = carregarDestaquesDaHome(generos, ordenacao, selecaoConteudo)
        val ordenacaoSecundaria = if (ordenacao == Ordenacao.RELEVANCIA) {
            Ordenacao.MENOR_PRECO
        } else ordenacao
        val menoresPrecos = paginaDaFileira(
            tituloDescontos, generos, ordenacaoSecundaria, selecaoConteudo, 0, 20
        ).jogos
        return listOfNotNull(
            Fileira(tituloOfertas, subtitulo, populares).takeIf { populares.isNotEmpty() },
            Fileira(tituloDescontos, subtitulo, menoresPrecos).takeIf { menoresPrecos.isNotEmpty() }
        )
    }

    private suspend fun carregarDestaquesDaHome(
        generos: Set<Genero>,
        ordenacao: Ordenacao,
        selecaoConteudo: SelecaoConteudo
    ): List<Jogo> {
        if (ordenacao != Ordenacao.RELEVANCIA ||
            selecaoConteudo != SelecaoConteudo.JOGOS || generos.isEmpty()) {
            return paginaDaFileira(
                tituloOfertas, generos, ordenacao, selecaoConteudo, 0, TAMANHO_PAGINA_HOME
            ).jogos
        }

        val encontrados = mutableListOf<Jogo>()
        for (offsetPagina in 0 until
            MAXIMO_PAGINAS_DESTAQUES * TAMANHO_PAGINA_HOME step TAMANHO_PAGINA_HOME) {
            val pagina = paginaDaFileira(
                tituloOfertas,
                generos,
                Ordenacao.RELEVANCIA,
                SelecaoConteudo.JOGOS,
                offsetPagina,
                TAMANHO_PAGINA_HOME
            )
            encontrados += pagina.jogos
            if (encontrados.size >= MINIMO_DESTAQUES_HOME) {
                return encontrados.distinctBy { it.idItad ?: it.id.toString() }
                    .take(TAMANHO_PAGINA_HOME)
            }
            if (!pagina.temMais) break
        }

        if (encontrados.isNotEmpty()) {
            return encontrados.distinctBy { it.idItad ?: it.id.toString() }
                .take(TAMANHO_PAGINA_HOME)
        }

        // O ranking não aceita gênero. Se as primeiras páginas não tiverem nenhuma
        // correspondência, a lista de ofertas por relevância mantém a fileira visível.
        return carregar(
            caminhoOfertas(
                generos,
                Ordenacao.RELEVANCIA,
                SelecaoConteudo.JOGOS,
                offset = 0,
                limite = TAMANHO_PAGINA_HOME
            ),
            listaOfertas = true
        )
    }

    override suspend fun buscarPorTitulo(texto: String): List<Jogo> {
        if (texto.isBlank()) return emptyList()
        val titulo = URLEncoder.encode(texto.trim(), "UTF-8")
        return carregar("/games/search/v1?title=$titulo&results=20", listaOfertas = false)
    }

    override suspend fun jogosDaFileira(
        tituloDaFileira: String,
        generos: Set<Genero>,
        ordenacao: Ordenacao,
        selecaoConteudo: SelecaoConteudo
    ): List<Jogo> = paginaDaFileira(
        tituloDaFileira, generos, ordenacao, selecaoConteudo, 0, 20
    ).jogos

    override suspend fun paginaDaFileira(
        tituloDaFileira: String,
        generos: Set<Genero>,
        ordenacao: Ordenacao,
        selecaoConteudo: SelecaoConteudo,
        offset: Int,
        limite: Int
    ): PaginaDeJogos {
        if (tituloDaFileira != tituloOfertas && tituloDaFileira != tituloDescontos) {
            return PaginaDeJogos(emptyList(), temMais = false)
        }
        val offsetSeguro = offset.coerceAtLeast(0)
        val limiteSeguro = limite.coerceIn(1, 200)
        val carregados = if (
            ordenacao == Ordenacao.RELEVANCIA && selecaoConteudo == SelecaoConteudo.JOGOS
        ) {
            carregar(
                "/stats/most-popular/v1?offset=$offsetSeguro&limit=$limiteSeguro",
                listaOfertas = false,
                somenteJogos = true
            )
        } else {
            carregar(
                caminhoOfertas(generos, ordenacao, selecaoConteudo, offsetSeguro, limiteSeguro),
                listaOfertas = true
            )
        }
        val filtrados = if (ordenacao != Ordenacao.RELEVANCIA || generos.isEmpty()) carregados else {
            carregados.filter { jogo -> generos.any { it.corresponde(jogo.tags) } }
        }
        return PaginaDeJogos(filtrados, temMais = carregados.size == limiteSeguro)
    }

    private fun caminhoOfertas(
        generos: Set<Genero>,
        ordenacao: Ordenacao,
        selecaoConteudo: SelecaoConteudo,
        offset: Int,
        limite: Int
    ): String {
        val filtro = JSONObject().put("type", JSONArray(selecaoConteudo.tiposItad))
        if (generos.isNotEmpty()) {
            val tags = generos.sortedBy { it.ordinal }.map { it.tag }
            filtro.put("tagsUnion", JSONArray(tags))
        }
        val sort = when (ordenacao) {
            Ordenacao.MENOR_PRECO -> "&sort=price"
            Ordenacao.MAIOR_DESCONTO -> "&sort=-cut"
            else -> ""
        }
        return "/deals/v2?country=BR&offset=$offset&limit=$limite&nondeals=false$sort&filter=" +
            URLEncoder.encode(filtro.toString(), "UTF-8")
    }

    private suspend fun carregar(
        caminho: String,
        listaOfertas: Boolean,
        somenteJogos: Boolean = false
    ): List<Jogo> = mutex.withLock {
        cache[caminho]?.takeIf { agora() - it.instante < 300_000 }?.let { return@withLock it.jogos }
        val resposta = consultar(caminho, null)
        val bases = (if (listaOfertas) JSONObject(resposta).getJSONArray("list") else JSONArray(resposta))
            .objetos()
            .filter { !somenteJogos || it.optString("type") == "game" }
            .distinctBy { it.getString("id") }
        if (bases.isEmpty()) return@withLock emptyList()
        val ids = JSONArray(bases.map { it.getString("id") }).toString()
        val precos = JSONArray(consultar("/games/prices/v3?country=BR&vouchers=false", ids))
            .objetos().associateBy { it.getString("id") }
        val jogos = coroutineScope {
            bases.map { base ->
                async {
                    limite.withPermit {
                        val id = base.getString("id")
                        val detalhes = try {
                            JSONObject(consultar("/games/info/v2?id=${URLEncoder.encode(id, "UTF-8")}", null))
                        } catch (erro: ErroItad) {
                            if (erro.tipo != ErroItad.Tipo.NAO_ENCONTRADO) throw erro
                            JSONObject()
                        }
                        MapeadorItad.jogo(base, detalhes, precos[id])
                    }
                }
            }.awaitAll()
        }
        cache[caminho] = Entrada(agora(), jogos)
        if (cache.size > 20) cache.remove(cache.keys.first())
        jogos
    }
}
