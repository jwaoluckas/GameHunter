package soares.lucas.joao.com.example.gamehunter.dados

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Loja
import soares.lucas.joao.com.example.gamehunter.modelo.Oferta
import soares.lucas.joao.com.example.gamehunter.modelo.Plataforma
import soares.lucas.joao.com.example.gamehunter.modelo.TipoConteudo
import java.io.IOException

// Favoritos privados deste dispositivo. Cada UID do Firebase possui uma lista separada,
// mas os jogos nao sao enviados ao Firestore e funcionam mesmo sem internet.
class RepositorioFavoritos(contexto: Context) {

    companion object {
        private const val NOME_PREFERENCIAS = "favoritos_gamehunter"
        private const val PREFIXO_CHAVE = "favoritos_"
        private const val CAMPO_ADICIONADO_EM = "adicionadoEm"
        private val bloqueio = Any()
    }

    private val autenticacao = FirebaseAuth.getInstance()
    private val preferencias = contexto.applicationContext.getSharedPreferences(
        NOME_PREFERENCIAS,
        Context.MODE_PRIVATE
    )

    private fun uidAtual(): String? = autenticacao.currentUser?.uid

    private fun chave(uid: String): String = "$PREFIXO_CHAVE$uid"

    private fun idDoJogo(jogo: Jogo): String = jogo.idItad ?: jogo.id.toString()

    fun verificar(jogo: Jogo, aoTerminar: (Boolean) -> Unit, aoFalhar: () -> Unit) {
        val uid = uidAtual() ?: return aoFalhar()
        runCatching {
            listar(uid).any { idDoJogo(it) == idDoJogo(jogo) }
        }.onSuccess(aoTerminar).onFailure { aoFalhar() }
    }

    fun adicionar(jogo: Jogo, aoFalhar: () -> Unit) {
        val uid = uidAtual() ?: return aoFalhar()
        runCatching {
            synchronized(bloqueio) {
                val atuais = listarComData(uid).filterNot { idDoJogo(it.first) == idDoJogo(jogo) }
                salvar(uid, listOf(jogo to System.currentTimeMillis()) + atuais)
            }
        }.onFailure { aoFalhar() }
    }

    fun remover(jogo: Jogo, aoFalhar: () -> Unit) {
        val uid = uidAtual() ?: return aoFalhar()
        runCatching {
            synchronized(bloqueio) {
                val atuais = listarComData(uid).filterNot { idDoJogo(it.first) == idDoJogo(jogo) }
                salvar(uid, atuais)
            }
        }.onFailure { aoFalhar() }
    }

    // Emite a lista atual e continua observando alteracoes feitas neste processo.
    fun observar(
        aoReceber: (List<Jogo>) -> Unit,
        aoFalhar: () -> Unit
    ): AutoCloseable? {
        val uid = uidAtual() ?: return null
        val chaveUsuario = chave(uid)

        fun emitir() {
            runCatching { listar(uid) }
                .onSuccess(aoReceber)
                .onFailure { aoFalhar() }
        }

        val ouvinte = SharedPreferences.OnSharedPreferenceChangeListener { _, chaveAlterada ->
            if (chaveAlterada == chaveUsuario) emitir()
        }
        preferencias.registerOnSharedPreferenceChangeListener(ouvinte)
        emitir()

        return AutoCloseable {
            preferencias.unregisterOnSharedPreferenceChangeListener(ouvinte)
        }
    }

    internal suspend fun listarParaAlertas(): List<Jogo> {
        val uid = uidAtual() ?: return emptyList()
        return listar(uid)
    }

    private fun listar(uid: String): List<Jogo> = listarComData(uid).map { it.first }

    private fun listarComData(uid: String): List<Pair<Jogo, Long>> {
        val texto = preferencias.getString(chave(uid), null) ?: return emptyList()
        val array = JSONArray(texto)
        return (0 until array.length()).mapNotNull { indice ->
            runCatching {
                val item = array.getJSONObject(indice)
                paraJogo(item.getJSONObject("jogo")) to item.optLong(CAMPO_ADICIONADO_EM)
            }.getOrNull()
        }.sortedByDescending { it.second }
    }

    private fun salvar(uid: String, favoritos: List<Pair<Jogo, Long>>) {
        val array = JSONArray()
        favoritos.forEach { (jogo, adicionadoEm) ->
            array.put(JSONObject().apply {
                put(CAMPO_ADICIONADO_EM, adicionadoEm)
                put("jogo", paraJson(jogo))
            })
        }
        if (!preferencias.edit().putString(chave(uid), array.toString()).commit()) {
            throw IOException("Nao foi possivel salvar os favoritos locais")
        }
    }

    private fun paraJson(jogo: Jogo): JSONObject = JSONObject().apply {
        put("id", jogo.id)
        put("idItad", jogo.idItad ?: JSONObject.NULL)
        put("tipoConteudo", jogo.tipoConteudo.name)
        put("titulo", jogo.titulo)
        put("capaUrl", jogo.capaUrl ?: JSONObject.NULL)
        put("nota", numeroOuNulo(jogo.nota))
        put("precoLancamento", numeroOuNulo(jogo.precoLancamento))
        put("plataformas", JSONArray(jogo.plataformas.map { it.name }))
        put("genero", jogo.genero)
        put("desenvolvedora", jogo.desenvolvedora)
        put("anoLancamento", jogo.anoLancamento)
        put("resumo", jogo.resumo)
        put("precoEmDolar", numeroOuNulo(jogo.precoEmDolar))
        put("tags", JSONArray(jogo.tags.toList()))
        put("ofertas", JSONArray().apply {
            jogo.ofertas.forEach { oferta ->
                put(JSONObject().apply {
                    put("loja", oferta.loja?.name ?: JSONObject.NULL)
                    put("nomeLoja", oferta.nomeLoja)
                    put("plataforma", oferta.plataforma)
                    put("preco", numeroOuNulo(oferta.preco))
                    put("precoRegular", numeroOuNulo(oferta.precoRegular))
                    put("url", oferta.url ?: JSONObject.NULL)
                })
            }
        })
    }

    private fun paraJogo(json: JSONObject): Jogo {
        val ofertasJson = json.optJSONArray("ofertas") ?: JSONArray()
        val ofertas = (0 until ofertasJson.length()).mapNotNull { indice ->
            runCatching {
                val item = ofertasJson.getJSONObject(indice)
                val loja = item.textoOuNulo("loja")
                    ?.let { nome -> Loja.entries.find { it.name == nome } }
                Oferta(
                    loja = loja,
                    preco = item.numeroOuNaN("preco"),
                    nomeLoja = item.optString("nomeLoja", loja?.nome.orEmpty()),
                    plataforma = item.optString("plataforma", loja?.plataforma ?: "PC"),
                    url = item.textoOuNulo("url"),
                    precoRegular = item.numeroOuNaN("precoRegular")
                )
            }.getOrNull()
        }

        return Jogo(
            id = json.optInt("id", json.optString("titulo").hashCode()),
            titulo = json.getString("titulo"),
            capaUrl = json.textoOuNulo("capaUrl"),
            nota = json.numeroOuNaN("nota"),
            precoLancamento = json.numeroOuNaN("precoLancamento"),
            plataformas = json.textos("plataformas")
                .mapNotNull { nome -> Plataforma.entries.find { it.name == nome } },
            genero = json.optString("genero"),
            desenvolvedora = json.optString("desenvolvedora"),
            anoLancamento = json.optInt("anoLancamento"),
            resumo = json.optString("resumo"),
            precoEmDolar = json.numeroOuNaN("precoEmDolar"),
            ofertas = ofertas,
            idItad = json.textoOuNulo("idItad"),
            tags = json.textos("tags").toSet(),
            tipoConteudo = json.optString("tipoConteudo")
                .let { nome -> TipoConteudo.entries.find { it.name == nome } }
                ?: TipoConteudo.JOGO
        )
    }

    private fun numeroOuNulo(valor: Double): Any =
        valor.takeIf { it.isFinite() } ?: JSONObject.NULL

    private fun JSONObject.numeroOuNaN(campo: String): Double =
        if (isNull(campo)) Double.NaN else optDouble(campo, Double.NaN)

    private fun JSONObject.textoOuNulo(campo: String): String? =
        if (isNull(campo) || !has(campo)) null else optString(campo).takeIf { it.isNotBlank() }

    private fun JSONObject.textos(campo: String): List<String> {
        val array = optJSONArray(campo) ?: return emptyList()
        return (0 until array.length()).mapNotNull { indice -> array.optString(indice).takeIf { it.isNotBlank() } }
    }
}
