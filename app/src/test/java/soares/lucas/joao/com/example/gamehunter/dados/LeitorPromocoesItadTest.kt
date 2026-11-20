package soares.lucas.joao.com.example.gamehunter.dados

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitorPromocoesItadTest {
    @Test
    fun calculaMediaEDescontoComoOsCards() {
        val resposta = """[
            {"id":"jogo-1","deals":[
                {"price":{"amount":50,"currency":"BRL"},"regular":{"amount":100,"currency":"BRL"}},
                {"price":{"amount":70,"currency":"BRL"},"regular":{"amount":100,"currency":"BRL"}}
            ]}
        ]"""

        val promocao = LeitorPromocoesItad.ler(resposta).getValue("jogo-1")

        assertEquals(60.0, promocao.precoMedio, 0.001)
        assertEquals(40, promocao.desconto)
        assertTrue(promocao.emPromocao)
    }

    @Test
    fun ignoraMoedaEstrangeiraECupom() {
        val resposta = """[
            {"id":"jogo-2","deals":[
                {"price":{"amount":10,"currency":"USD"},"regular":{"amount":20,"currency":"USD"}},
                {"price":{"amount":30,"currency":"BRL"},"regular":{"amount":60,"currency":"BRL"},"voucher":"CUPOM"}
            ]}
        ]"""

        val promocao = LeitorPromocoesItad.ler(resposta).getValue("jogo-2")

        assertTrue(promocao.precoMedio.isNaN())
        assertEquals(0, promocao.desconto)
        assertFalse(promocao.emPromocao)
    }
}
