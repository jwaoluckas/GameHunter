package soares.lucas.joao.com.example.gamehunter.dados

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import soares.lucas.joao.com.example.gamehunter.modelo.Loja
import soares.lucas.joao.com.example.gamehunter.modelo.Plataforma
import soares.lucas.joao.com.example.gamehunter.modelo.Genero
import soares.lucas.joao.com.example.gamehunter.modelo.Filtros
import java.net.URLDecoder

class IntegracaoItadTest {
    private val id = "018d937f-07fc-72ed-8517-d8e24cb1eb22"
    private fun base() = JSONObject("""{"id":"$id","title":"Jogo de teste","assets":{"boxart":"https://example.com/capa.jpg"}}""")
    private fun oferta(moeda: String = "BRL", valor: Double = 50.0, loja: Int = 61) = JSONObject("""
        {"shop":{"id":$loja,"name":"Loja de teste"},
         "price":{"amount":$valor,"currency":"$moeda"},
         "regular":{"amount":100,"currency":"$moeda"},"voucher":null,
         "url":"https://itad.link/oferta?affiliate=preservar"}
    """)
    private fun precos(vararg ofertas: JSONObject) = JSONObject().put("id", id).put("deals", JSONArray(ofertas.toList()))

    @Test fun generoConsideraTodasAsTagsECombinaComPrecoELoja() {
        val jogo = MapeadorItad.jogo(base(), JSONObject("""{"tags":["Singleplayer","Action","Adventure"]}"""), precos(oferta()))
        assertEquals(setOf("Singleplayer", "Action", "Adventure"), jogo.tags)
        assertEquals(listOf(jogo), Filtros(generos = setOf(Genero.AVENTURA)).aplicar(listOf(jogo)))
        assertEquals(listOf(jogo), Filtros(generos = setOf(Genero.RPG, Genero.ACAO)).aplicar(listOf(jogo)))
        assertTrue(Filtros(generos = setOf(Genero.RPG)).aplicar(listOf(jogo)).isEmpty())
        assertTrue(Filtros(generos = setOf(Genero.ACAO), precoMaximo = 20).aplicar(listOf(jogo)).isEmpty())
        assertTrue(Filtros(generos = setOf(Genero.ACAO), lojas = setOf(Loja.NUUVEM)).aplicar(listOf(jogo)).isEmpty())
        assertEquals(listOf(jogo), Filtros().aplicar(listOf(jogo)))
    }

    @Test fun jogoSemTagsSoApareceSemFiltroDeGenero() {
        val jogo = MapeadorItad.jogo(base(), JSONObject(), null)
        assertTrue(Filtros(generos = setOf(Genero.ACAO)).aplicar(listOf(jogo)).isEmpty())
        assertEquals(listOf(jogo), Filtros().aplicar(listOf(jogo)))
    }

    @Test fun enviaGenerosNaApiEPreservaSelecaoNoVerTudoSemMisturarCache() = runBlocking {
        val consultas = mutableListOf<String>()
        val repositorio = RepositorioItad({ caminho, _ ->
            when {
                caminho.startsWith("/deals/") -> {
                    consultas += caminho
                    JSONObject().put("list", JSONArray().put(base())).toString()
                }
                caminho.startsWith("/games/prices/") -> JSONArray().put(precos(oferta())).toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte")
        val generos = setOf(Genero.AVENTURA, Genero.ACAO)
        val fileiras = repositorio.fileiras(generos)
        assertEquals(fileiras.first().jogos, repositorio.jogosDaFileira("Ofertas", generos))
        assertEquals(1, consultas.size)
        val filtro = JSONObject(URLDecoder.decode(consultas.first().substringAfter("&filter="), "UTF-8"))
        assertEquals(listOf("Action", "Adventure"), (0..1).map { filtro.getJSONArray("tagsUnion").getString(it) })
        repositorio.fileiras()
        assertEquals(2, consultas.size)
        assertFalse(consultas.last().contains("filter="))
        repositorio.fileiras(setOf(Genero.ACAO, Genero.AVENTURA))
        assertEquals(2, consultas.size)
    }

    @Test fun usaPrecoRegularComoOriginalEMantemMediaDasOfertas() {
        val info = JSONObject("""{"reviews":[{"source":"Metascore","score":87}],"releaseDate":"2020-01-02",
            "tags":["RPG"],"developers":[{"name":"Estúdio"}]}""")
        val jogo = MapeadorItad.jogo(base(), info, precos(oferta(), oferta(valor = 70.0, loja = 50)))
        assertEquals(id, jogo.idItad)
        assertEquals(60.0, jogo.precoMedioHoje, 0.001)
        assertEquals(40, jogo.desconto)
        assertEquals(8.7, jogo.nota, 0.001)
        assertEquals(100.0, jogo.precoLancamento, 0.001)
        assertEquals(listOf(Plataforma.PC), jogo.plataformas)
        assertEquals(Loja.STEAM, jogo.ofertas.first().loja)
        assertEquals("https://itad.link/oferta?affiliate=preservar", jogo.ofertas.first().url)
        assertEquals(2020, jogo.anoLancamento)
    }

    @Test fun ignoraMoedaEstrangeiraCupomEPrecoInvalido() {
        val jogo = MapeadorItad.jogo(base(), JSONObject(), precos(
            oferta("USD"), oferta(valor = -1.0), oferta().put("voucher", "CUPOM"), oferta(valor = 0.0)
        ))
        assertEquals(1, jogo.ofertas.size)
        assertEquals(0.0, jogo.precoMedioHoje, 0.001) // Grátis é diferente de indisponível.
    }

    @Test fun preservaLojaForaDoEnum() {
        val jogo = MapeadorItad.jogo(base(), JSONObject(), precos(oferta(loja = 9999)))
        assertNull(jogo.ofertas.single().loja)
        assertEquals("Loja de teste", jogo.ofertas.single().nomeLoja)
    }

    @Test fun camposAusentesNaoViramValoresFicticios() {
        val jogo = MapeadorItad.jogo(base(), JSONObject(), null)
        assertTrue(jogo.nota.isNaN())
        assertTrue(jogo.precoMedioHoje.isNaN())
        assertTrue(jogo.precoLancamento.isNaN())
        assertTrue(jogo.precoEmDolar.isNaN())
        assertEquals(0, jogo.desconto)
    }

    @Test fun buscaCodificaTituloEReutilizaCacheAteExpirar() = runBlocking {
        var tempo = 0L
        val chamadas = mutableListOf<Pair<String, String?>>()
        val repositorio = RepositorioItad({ caminho, corpo ->
            chamadas += caminho to corpo
            when {
                caminho.startsWith("/games/search/") -> JSONArray().put(base()).toString()
                caminho.startsWith("/games/prices/") -> JSONArray().put(precos(oferta())).toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte", { tempo })
        val primeiro = repositorio.buscarPorTitulo("A & B")
        assertEquals(1, primeiro.size)
        assertTrue(chamadas.first().first.contains("title=A+%26+B"))
        assertEquals(id, JSONArray(chamadas[1].second!!).getString(0))
        assertEquals(primeiro, repositorio.buscarPorTitulo("A & B"))
        assertEquals(3, chamadas.size)
        tempo = 300_001
        repositorio.buscarPorTitulo("A & B")
        assertEquals(6, chamadas.size)
    }

    @Test fun fileiraCompletaReutilizaDadosDaInicial() = runBlocking {
        var chamadas = 0
        val repositorio = RepositorioItad({ caminho, _ ->
            chamadas++
            when {
                caminho.startsWith("/deals/") -> JSONObject().put("list", JSONArray().put(base())).toString()
                caminho.startsWith("/games/prices/") -> JSONArray().put(precos(oferta())).toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte")
        val fileiras = repositorio.fileiras()
        assertEquals(fileiras.first().jogos, repositorio.jogosDaFileira("Ofertas"))
        assertEquals(3, chamadas)
    }

    @Test fun falhaDeRedeNaoViraCatalogoFicticioNemEntraNoCache() = runBlocking {
        var chamadas = 0
        val repositorio = RepositorioItad({ _, _ ->
            chamadas++
            throw ErroItad(ErroItad.Tipo.LIMITE)
        }, "Ofertas", "Descontos", "Fonte")
        repeat(2) {
            try {
                repositorio.fileiras()
                fail("Deveria propagar o erro")
            } catch (erro: ErroItad) {
                assertEquals(ErroItad.Tipo.LIMITE, erro.tipo)
            }
        }
        assertEquals(2, chamadas)
    }

    @Test fun cancelamentoNaoViraResultadoVazio() = runBlocking {
        val repositorio = RepositorioItad({ _, _ -> throw CancellationException() }, "Ofertas", "Descontos", "Fonte")
        try {
            repositorio.fileiras()
            fail("Deveria propagar o cancelamento")
        } catch (_: CancellationException) {
            // O ciclo de vida da tela pode cancelar o carregamento.
        }
    }

    @Test fun chaveVaziaFalhaAntesDeAcessarARede() = runBlocking {
        try {
            ClienteItad("").consultar("/deals/v2")
            fail("Deveria pedir configuração")
        } catch (erro: ErroItad) {
            assertEquals(ErroItad.Tipo.SEM_CHAVE, erro.tipo)
        }
    }
}
