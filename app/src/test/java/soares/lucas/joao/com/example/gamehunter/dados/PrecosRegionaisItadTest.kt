package soares.lucas.joao.com.example.gamehunter.dados

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import soares.lucas.joao.com.example.gamehunter.componentes.CarregadorPrecosRegionais
import soares.lucas.joao.com.example.gamehunter.modelo.EstadoPrecoRegional
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

class PrecosRegionaisItadTest {
    private fun oferta(moeda: String, valor: Double, cupom: String? = null) = JSONObject()
        .put("price", JSONObject().put("currency", moeda).put("amount", valor))
        .put("voucher", cupom ?: JSONObject.NULL)
    private fun resposta(vararg ofertas: JSONObject, id: String = "jogo") = JSONArray()
        .put(JSONObject().put("id", id).put("deals", JSONArray(ofertas.toList()))).toString()

    @Test fun calculaMediaSomenteDaMoedaPedida() {
        val json = resposta(oferta("USD", 10.0), oferta("USD", 20.0), oferta("EUR", 100.0))
        assertEquals(15.0, PrecosRegionaisItad.mediaRegional(json, "jogo", Moeda.USD)!!, 0.001)
        assertNull(PrecosRegionaisItad.mediaRegional(json, "jogo", Moeda.BRL))
        assertNull(PrecosRegionaisItad.mediaRegional(json, "outro-jogo", Moeda.USD))
    }

    @Test fun naoApresentaDolarComoPesoArgentino() {
        assertNull(PrecosRegionaisItad.mediaRegional(resposta(oferta("USD", 20.0)), "jogo", Moeda.ARS))
    }

    @Test fun zeroEhGratisENaoAusenciaDeOferta() {
        assertEquals(0.0, PrecosRegionaisItad.mediaRegional(resposta(oferta("JPY", 0.0)), "jogo", Moeda.JPY)!!, 0.0)
        assertNull(PrecosRegionaisItad.mediaRegional(resposta(), "jogo", Moeda.JPY))
        assertNull(PrecosRegionaisItad.mediaRegional("[]", "jogo", Moeda.JPY))
    }

    @Test fun descartaCuponsPrecosNegativosEAusentes() {
        val json = resposta(oferta("CAD", 1.0, "CUPOM"), oferta("CAD", -2.0), JSONObject(), oferta("CAD", 30.0))
        assertEquals(30.0, PrecosRegionaisItad.mediaRegional(json, "jogo", Moeda.CAD)!!, 0.0)
    }

    @Test fun consultaPaisCorretoECriaCacheSeparadoPorJogoEMoeda() = runBlocking {
        var tempo = 0L
        val chamadas = mutableListOf<String>()
        val api = PrecosRegionaisItad({ caminho, corpo ->
            chamadas += caminho
            val id = JSONArray(corpo!!).getString(0)
            resposta(oferta("EUR", 20.0), oferta("GBP", 30.0), id = id)
        }, { tempo })
        assertEquals(20.0, api.consultarPreco("jogo", Moeda.EUR)!!, 0.0)
        assertTrue(chamadas.single().contains("country=DE&vouchers=false"))
        api.consultarPreco("jogo", Moeda.EUR)
        assertEquals(1, chamadas.size)
        assertEquals(30.0, api.consultarPreco("jogo", Moeda.GBP)!!, 0.0)
        assertTrue(chamadas.last().contains("country=GB"))
        api.consultarPreco("outro", Moeda.EUR)
        assertEquals(3, chamadas.size)
        tempo = 300_001
        api.consultarPreco("jogo", Moeda.EUR)
        assertEquals(4, chamadas.size)
    }

    @Test fun guardaAusenciaDeOfertasNoCacheMasNaoFalhas() = runBlocking {
        var chamadas = 0
        val api = PrecosRegionaisItad({ _, _ ->
            chamadas++
            if (chamadas == 1) throw ErroItad(ErroItad.Tipo.LIMITE)
            "[]"
        })
        try {
            api.consultarPreco("jogo", Moeda.MXN)
            fail("Deveria propagar falha")
        } catch (_: ErroItad) { }
        assertNull(api.consultarPreco("jogo", Moeda.MXN))
        assertNull(api.consultarPreco("jogo", Moeda.MXN))
        assertEquals(2, chamadas)
    }

    @Test fun cancelamentoNaoViraPrecoIndisponivelNoCache() = runBlocking {
        val api = PrecosRegionaisItad({ _, _ -> throw CancellationException() })
        try {
            api.consultarPreco("jogo", Moeda.USD)
            fail("Deveria cancelar")
        } catch (_: CancellationException) { }
    }

    @Test fun falhaParcialMantemDemaisMoedasEPermiteTentarNovamente() = runBlocking {
        var chamadasUsd = 0
        val repo = object : RepositorioDeJogos by RepositorioDeJogosDeExemplo {
            override suspend fun precoRegional(idItad: String, moeda: Moeda): Double? {
                if (moeda == Moeda.USD && ++chamadasUsd == 1) throw ErroItad(ErroItad.Tipo.LIMITE)
                return if (moeda == Moeda.JPY) null else 10.0
            }
        }
        var estados: Map<Moeda, EstadoPrecoRegional> = emptyMap()
        val carregador = CarregadorPrecosRegionais(this, repo, "jogo") { estados = it }
        carregador.carregar(listOf(Moeda.USD, Moeda.BRL, Moeda.JPY))
        assertTrue(estados.values.all { it is EstadoPrecoRegional.Carregando })
        yield()
        assertTrue(estados[Moeda.USD] is EstadoPrecoRegional.Falha)
        assertEquals(EstadoPrecoRegional.Disponivel(10.0), estados[Moeda.BRL])
        assertEquals(EstadoPrecoRegional.SemOfertas, estados[Moeda.JPY])
        carregador.carregar(listOf(Moeda.USD))
        yield()
        assertEquals(EstadoPrecoRegional.Disponivel(10.0), estados[Moeda.USD])
        carregador.carregar(listOf(Moeda.USD))
        assertEquals(2, chamadasUsd)
    }

    @Test fun fecharModalCancelaConsultasSemAtualizarTelaFechada() = runBlocking {
        val suspensao = CompletableDeferred<Double?>()
        val repo = object : RepositorioDeJogos by RepositorioDeJogosDeExemplo {
            override suspend fun precoRegional(idItad: String, moeda: Moeda) = suspensao.await()
        }
        val escopo = CoroutineScope(coroutineContext + Job(coroutineContext[Job]))
        var atualizacoes = 0
        val carregador = CarregadorPrecosRegionais(escopo, repo, "jogo") { atualizacoes++ }
        carregador.carregar(listOf(Moeda.USD))
        yield()
        escopo.cancel()
        suspensao.complete(10.0)
        yield()
        assertEquals(1, atualizacoes)
    }
}
