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
import java.net.URLEncoder

// Cache de cinco minutos, limitado a 20 consultas. A lista inicial é uma seleção de
// ofertas, não um ranking de vendas nem uma recomendação baseada no perfil.
internal class RepositorioItad(
    private val consultar: suspend (String, String?) -> String,
    private val tituloOfertas: String,
    private val tituloDescontos: String,
    private val subtitulo: String,
    private val agora: () -> Long = { System.nanoTime() / 1_000_000 }
) : RepositorioDeJogos {
    private data class Entrada(val instante: Long, val jogos: List<Jogo>)
    private val cache = LinkedHashMap<String, Entrada>()
    private val mutex = Mutex()
    private val limite = Semaphore(3)
    private val precosRegionais = PrecosRegionaisItad(consultar, agora)

    override suspend fun precoRegional(idItad: String, moeda: Moeda): Double? =
        precosRegionais.consultarPreco(idItad, moeda)

    override suspend fun fileiras(generos: Set<Genero>): List<Fileira> {
        val filtro = if (generos.isEmpty()) "" else {
            val tags = generos.sortedBy { it.ordinal }.map { it.tag }
            val json = JSONObject().put("tagsUnion", JSONArray(tags)).toString()
            "&filter=" + URLEncoder.encode(json, "UTF-8")
        }
        val jogos = carregar("/deals/v2?country=BR&limit=20&nondeals=false$filtro", listaOfertas = true)
        if (jogos.isEmpty()) return emptyList()
        return listOf(
            Fileira(tituloOfertas, subtitulo, jogos),
            Fileira(tituloDescontos, subtitulo, jogos.sortedByDescending { it.desconto })
        )
    }

    override suspend fun buscarPorTitulo(texto: String): List<Jogo> {
        if (texto.isBlank()) return emptyList()
        val titulo = URLEncoder.encode(texto.trim(), "UTF-8")
        return carregar("/games/search/v1?title=$titulo&results=20", listaOfertas = false)
    }

    override suspend fun jogosDaFileira(tituloDaFileira: String, generos: Set<Genero>): List<Jogo> =
        fileiras(generos).firstOrNull { it.titulo == tituloDaFileira }?.jogos.orEmpty()

    private suspend fun carregar(caminho: String, listaOfertas: Boolean): List<Jogo> = mutex.withLock {
        cache[caminho]?.takeIf { agora() - it.instante < 300_000 }?.let { return@withLock it.jogos }
        val resposta = consultar(caminho, null)
        val bases = (if (listaOfertas) JSONObject(resposta).getJSONArray("list") else JSONArray(resposta))
            .objetos().distinctBy { it.getString("id") }
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
