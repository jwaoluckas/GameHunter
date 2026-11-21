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
import soares.lucas.joao.com.example.gamehunter.modelo.Ordenacao
import soares.lucas.joao.com.example.gamehunter.modelo.SelecaoConteudo
import soares.lucas.joao.com.example.gamehunter.modelo.TipoConteudo
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

    @Test fun filtroDeConteudoDiferenciaJogosEDlcs() {
        val jogo = MapeadorItad.jogo(base().put("type", "game"), JSONObject(), null)
        val dlc = MapeadorItad.jogo(
            JSONObject(base().toString()).put("id", "dlc-id").put("type", "dlc"),
            JSONObject(),
            null
        )
        assertEquals(TipoConteudo.JOGO, jogo.tipoConteudo)
        assertEquals(TipoConteudo.DLC, dlc.tipoConteudo)
        assertEquals(listOf(jogo), Filtros().aplicar(listOf(jogo, dlc)))
        assertEquals(listOf(dlc), Filtros(selecaoConteudo = SelecaoConteudo.DLC).aplicar(listOf(jogo, dlc)))
        assertEquals(
            listOf(jogo, dlc),
            Filtros(selecaoConteudo = SelecaoConteudo.TODOS).aplicar(listOf(jogo, dlc))
        )
    }

    @Test fun usaRankingPopularPreservaRelevanciaEExcluiDlc() = runBlocking {
        val consultas = mutableListOf<String>()
        val consultasDePreco = mutableListOf<String>()
        val segundoId = "118d937f-07fc-72ed-8517-d8e24cb1eb22"
        val segundo = JSONObject(base().toString()).put("id", segundoId).put("title", "Segundo jogo")
        val dlc = JSONObject(base().toString()).put("id", "218d937f-07fc-72ed-8517-d8e24cb1eb22")
            .put("title", "Conteudo adicional").put("type", "dlc")
        val repositorio = RepositorioItad({ caminho, _ ->
            when {
                caminho.startsWith("/stats/most-popular/") -> {
                    consultas += caminho
                    JSONArray()
                        .put(base().put("type", "game"))
                        .put(segundo.put("type", "game"))
                        .put(dlc)
                        .toString()
                }
                caminho.startsWith("/deals/") -> {
                    consultasDePreco += caminho
                    JSONObject().put("list", JSONArray().put(base().put("type", "game"))).toString()
                }
                caminho.startsWith("/games/prices/") -> JSONArray()
                    .put(precos(oferta()))
                    .put(JSONObject().put("id", segundoId).put("deals", JSONArray()))
                    .toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte")
        val fileiras = repositorio.fileiras()
        assertEquals(listOf("Jogo de teste", "Segundo jogo"), fileiras.first().jogos.map { it.titulo })
        assertEquals(fileiras.first().jogos, repositorio.jogosDaFileira("Ofertas"))
        assertEquals(1, consultas.size)
        assertEquals("/stats/most-popular/v1?offset=0&limit=20", consultas.single())
        assertTrue(consultasDePreco.single().contains("sort=price"))
    }

    @Test fun paginaPopularUsaOffsetEInformaQuandoPodeHaverMaisJogos() = runBlocking {
        val primeiroId = id
        val segundoId = "118d937f-07fc-72ed-8517-d8e24cb1eb22"
        val consultas = mutableListOf<String>()
        val repositorio = RepositorioItad({ caminho, _ ->
            when {
                caminho.startsWith("/stats/most-popular/") -> {
                    consultas += caminho
                    JSONArray()
                        .put(base().put("type", "game"))
                        .put(JSONObject(base().toString()).put("id", segundoId).put("type", "game"))
                        .toString()
                }
                caminho.startsWith("/games/prices/") -> JSONArray()
                    .put(JSONObject().put("id", primeiroId).put("deals", JSONArray()))
                    .put(JSONObject().put("id", segundoId).put("deals", JSONArray()))
                    .toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte")

        val pagina = repositorio.paginaDaFileira(
            "Ofertas", emptySet(), Ordenacao.RELEVANCIA, offset = 20, limite = 2
        )

        assertEquals(2, pagina.jogos.size)
        assertTrue(pagina.temMais)
        assertEquals("/stats/most-popular/v1?offset=20&limit=2", consultas.single())
    }

    @Test fun homeMantemDestaquesQuandoGeneroNaoEstaNaPrimeiraPaginaPopular() = runBlocking {
        val estrategiaId = "318d937f-07fc-72ed-8517-d8e24cb1eb22"
        val estrategia = JSONObject(base().toString())
            .put("id", estrategiaId)
            .put("title", "Jogo de estrategia")
            .put("type", "game")
        val consultasDeOfertas = mutableListOf<String>()
        val repositorio = RepositorioItad({ caminho, corpo ->
            when {
                caminho.startsWith("/stats/most-popular/") -> JSONArray()
                    .put(base().put("type", "game")).toString()
                caminho.startsWith("/deals/") -> {
                    consultasDeOfertas += caminho
                    JSONObject().put("list", JSONArray().put(estrategia)).toString()
                }
                caminho.startsWith("/games/prices/") -> {
                    val ids = JSONArray(corpo)
                    JSONArray((0 until ids.length()).map { indice ->
                        JSONObject().put("id", ids.getString(indice)).put("deals", JSONArray())
                    }).toString()
                }
                caminho.contains(estrategiaId) -> JSONObject().put("tags", JSONArray().put("Strategy")).toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte")

        val fileiras = repositorio.fileiras(setOf(Genero.ESTRATEGIA))

        assertEquals(2, fileiras.size)
        assertEquals("Jogo de estrategia", fileiras.first().jogos.single().titulo)
        assertTrue(consultasDeOfertas.any { !it.contains("sort=") })
        assertTrue(consultasDeOfertas.any { it.contains("sort=price") })
    }

    @Test fun maiorDescontoConsultaOutrosJogosComFiltroDeTipoEGenero() = runBlocking {
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

        repositorio.fileiras(setOf(Genero.ACAO), Ordenacao.MAIOR_DESCONTO)

        assertEquals(1, consultas.size)
        assertTrue(consultas.single().contains("sort=-cut"))
        val filtro = JSONObject(URLDecoder.decode(consultas.single().substringAfter("&filter="), "UTF-8"))
        assertEquals(1, filtro.getJSONArray("type").getInt(0))
        assertEquals("Action", filtro.getJSONArray("tagsUnion").getString(0))
    }

    @Test fun selecaoDlcUsaListaDeOfertasComTipoDois() = runBlocking {
        var caminhoDaLista = ""
        val repositorio = RepositorioItad({ caminho, _ ->
            when {
                caminho.startsWith("/deals/") -> {
                    caminhoDaLista = caminho
                    JSONObject().put("list", JSONArray().put(base().put("type", "dlc"))).toString()
                }
                caminho.startsWith("/games/prices/") -> JSONArray().put(precos(oferta())).toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte")

        repositorio.fileiras(emptySet(), Ordenacao.RELEVANCIA, SelecaoConteudo.DLC)

        assertTrue(caminhoDaLista.startsWith("/deals/"))
        val filtro = JSONObject(URLDecoder.decode(caminhoDaLista.substringAfter("&filter="), "UTF-8"))
        assertEquals(listOf(2), (0 until filtro.getJSONArray("type").length()).map {
            filtro.getJSONArray("type").getInt(it)
        })
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

    @Test fun mapeiaTodasAsLojasDisponiveisNoFiltro() {
        val lojas = listOf(
            61 to Loja.STEAM,
            16 to Loja.EPIC_GAMES,
            50 to Loja.NUUVEM,
            48 to Loja.MICROSOFT_STORE,
            35 to Loja.GOG,
            37 to Loja.HUMBLE_STORE
        )

        lojas.forEach { (idLoja, lojaEsperada) ->
            val jogo = MapeadorItad.jogo(base(), JSONObject(), precos(oferta(loja = idLoja)))
            assertEquals(lojaEsperada, jogo.ofertas.single().loja)
        }
    }

    @Test fun enviaLojasSelecionadasNaConsultaPaginada() = runBlocking {
        var caminhoDaLista = ""
        val repositorio = RepositorioItad({ caminho, _ ->
            when {
                caminho.startsWith("/deals/") -> {
                    caminhoDaLista = caminho
                    JSONObject().put("list", JSONArray().put(base().put("type", "game"))).toString()
                }
                caminho.startsWith("/games/prices/") -> JSONArray().put(precos(oferta(loja = 35))).toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte")

        repositorio.paginaDaFileira(
            "Descontos",
            ordenacao = Ordenacao.MENOR_PRECO,
            lojas = setOf(Loja.GOG, Loja.HUMBLE_STORE)
        )

        assertTrue(caminhoDaLista.contains("&shops=35,37&"))
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
                caminho.startsWith("/stats/most-popular/") -> JSONArray()
                    .put(base().put("type", "game")).toString()
                caminho.startsWith("/deals/") -> JSONObject()
                    .put("list", JSONArray().put(base().put("type", "game"))).toString()
                caminho.startsWith("/games/prices/") -> JSONArray().put(precos(oferta())).toString()
                else -> "{}"
            }
        }, "Ofertas", "Descontos", "Fonte")
        val fileiras = repositorio.fileiras()
        assertEquals(fileiras.first().jogos, repositorio.jogosDaFileira("Ofertas"))
        assertEquals(6, chamadas)
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
