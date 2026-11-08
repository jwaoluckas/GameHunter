package soares.lucas.joao.com.example.gamehunter.dados

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Consulta sob demanda, sem multiplicar as requisições da tela inicial.
// Falhas não entram no cache; ausência de ofertas entra e não vira preço zero.
internal class PrecosRegionaisItad(
    private val consultar: suspend (String, String?) -> String,
    private val agora: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private data class Chave(val id: String, val moeda: Moeda)
    private data class Entrada(val instante: Long, val valor: Double?)
    private val cache = LinkedHashMap<Chave, Entrada>()
    private val mutex = Mutex()
    private val limite = Semaphore(3)

    suspend fun consultarPreco(id: String, moeda: Moeda): Double? = limite.withPermit {
        val chave = Chave(id, moeda)
        mutex.withLock { cache[chave]?.takeIf { agora() - it.instante < 300_000 } }
            ?.let { return@withPermit it.valor }
        val resposta = consultar(
            "/games/prices/v3?country=${moeda.pais}&vouchers=false",
            JSONArray(listOf(id)).toString()
        )
        val valor = mediaRegional(resposta, id, moeda)
        mutex.withLock {
            cache[chave] = Entrada(agora(), valor)
            if (cache.size > 128) cache.remove(cache.keys.first())
        }
        valor
    }

    companion object {
        internal fun mediaRegional(resposta: String, id: String, moeda: Moeda): Double? {
            val jogo = JSONArray(resposta).objetos().firstOrNull { it.optString("id") == id }
                ?: return null
            val valores = jogo.optJSONArray("deals")?.objetos().orEmpty().mapNotNull { oferta ->
                if (!oferta.isNull("voucher") && oferta.optString("voucher").isNotBlank()) return@mapNotNull null
                val preco = oferta.optJSONObject("price") ?: return@mapNotNull null
                if (preco.optString("currency") != moeda.name) return@mapNotNull null
                preco.optDouble("amount", Double.NaN).takeIf { it.isFinite() && it >= 0 }
            }
            return valores.takeIf { it.isNotEmpty() }?.average()
        }
    }
}
