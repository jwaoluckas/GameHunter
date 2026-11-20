package soares.lucas.joao.com.example.gamehunter.dados

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.suspendCancellableCoroutine
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Loja
import soares.lucas.joao.com.example.gamehunter.modelo.Oferta
import soares.lucas.joao.com.example.gamehunter.modelo.Plataforma
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// FAVORITOS do usuário logado, salvos no Cloud Firestore (o mesmo banco usado pelo
// perfil). Cada usuário tem a sua subcoleção, ligada ao UID do Firebase Authentication:
//
//   usuarios/{uid}/favoritos/{idDoJogo}
//
// O id do documento é o id da IsThereAnyDeal (idItad), que identifica o jogo de forma
// estável; jogos sem idItad (dados de exemplo) usam o id numérico.
// Como o catálogo não tem uma busca "por id", o documento guarda um retrato do jogo
// com tudo o que o card e o modal exibem (título, capa, nota, preços, plataformas,
// ofertas...). Os preços ficam como estavam no momento em que o jogo foi favoritado.
//
// As funções usam callbacks (aoTerminar / aoFalhar), no mesmo padrão de
// addOnSuccessListener/addOnFailureListener já usado no Login, Cadastro e Configurações.
// Se não houver usuário logado, a operação falha direto pelo aoFalhar.
class RepositorioFavoritos {

    // Nomes da coleção/subcoleção e do campo usado para ordenar a lista
    companion object {
        private const val COLECAO_USUARIOS = "usuarios"
        private const val COLECAO_FAVORITOS = "favoritos"
        private const val CAMPO_ADICIONADO_EM = "adicionadoEm"
    }

    private val autenticacao = FirebaseAuth.getInstance()
    private val banco = FirebaseFirestore.getInstance()

    // Subcoleção de favoritos do usuário logado (null quando ninguém está logado)
    private fun colecaoDoUsuario(): CollectionReference? {
        val uid = autenticacao.currentUser?.uid ?: return null
        return banco.collection(COLECAO_USUARIOS).document(uid).collection(COLECAO_FAVORITOS)
    }

    private fun idDoDocumento(jogo: Jogo): String = jogo.idItad ?: jogo.id.toString()

    // ===== OPERAÇÕES =====
    // verificar: lê o documento do jogo para saber se ele já é favorito (usado ao abrir o modal)
    // adicionar: grava (ou regrava) o retrato do jogo
    // remover: apaga o documento do jogo
    // Sem internet, o Firestore guarda as gravações e as envia quando a conexão voltar;
    // o aoFalhar é chamado quando a operação é recusada (ex.: sessão ou regras de segurança)
    // e, na leitura, quando não há conexão nem cópia local.

    fun verificar(jogo: Jogo, aoTerminar: (Boolean) -> Unit, aoFalhar: () -> Unit) {
        val colecao = colecaoDoUsuario() ?: return aoFalhar()
        colecao.document(idDoDocumento(jogo)).get()
            .addOnSuccessListener { documento -> aoTerminar(documento.exists()) }
            .addOnFailureListener { aoFalhar() }
    }

    fun adicionar(jogo: Jogo, aoFalhar: () -> Unit) {
        val colecao = colecaoDoUsuario() ?: return aoFalhar()
        colecao.document(idDoDocumento(jogo)).set(paraDocumento(jogo))
            .addOnFailureListener { aoFalhar() }
    }

    fun remover(jogo: Jogo, aoFalhar: () -> Unit) {
        val colecao = colecaoDoUsuario() ?: return aoFalhar()
        colecao.document(idDoDocumento(jogo)).delete()
            .addOnFailureListener { aoFalhar() }
    }

    // Observa a lista de favoritos em TEMPO REAL, do mais recente para o mais antigo:
    // aoReceber é chamado com a lista atual e de novo a cada mudança (ex.: o usuário
    // desfavoritou pelo modal). Passar a Activity faz o Firestore remover o "ouvinte"
    // sozinho no onStop dela, por isso a tela deve chamar esta função no onStart.
    // Retorna null (sem chamar nada) quando não há usuário logado
    fun observar(
        activity: Activity,
        aoReceber: (List<Jogo>) -> Unit,
        aoFalhar: () -> Unit
    ): ListenerRegistration? {
        val colecao = colecaoDoUsuario() ?: return null
        return colecao.orderBy(CAMPO_ADICIONADO_EM, Query.Direction.DESCENDING)
            .addSnapshotListener(activity) { resultado, erro ->
                if (erro != null || resultado == null) {
                    aoFalhar()
                } else {
                    aoReceber(resultado.documents.mapNotNull { paraJogo(it) })
                }
            }
    }

    internal suspend fun listarParaAlertas(): List<Jogo> {
        val colecao = colecaoDoUsuario() ?: return emptyList()
        return suspendCancellableCoroutine { continuacao ->
            colecao.get()
                .addOnSuccessListener { resultado ->
                    if (continuacao.isActive) {
                        continuacao.resume(resultado.documents.mapNotNull { paraJogo(it) })
                    }
                }
                .addOnFailureListener { erro ->
                    if (continuacao.isActive) continuacao.resumeWithException(erro)
                }
        }
    }

    // ===== CONVERSÃO Jogo <-> documento do Firestore =====
    // Enums (plataformas e lojas) são salvos pelo nome; números ausentes (NaN, que
    // aparecem como travessão na tela) são salvos como null e voltam como NaN.
    // A data em que o jogo foi favoritado serve para ordenar a lista

    private fun paraDocumento(jogo: Jogo): Map<String, Any?> = mapOf(
        "id" to jogo.id,
        "idItad" to jogo.idItad,
        "titulo" to jogo.titulo,
        "capaUrl" to jogo.capaUrl,
        "nota" to numeroOuNulo(jogo.nota),
        "precoLancamento" to numeroOuNulo(jogo.precoLancamento),
        "plataformas" to jogo.plataformas.map { it.name },
        "genero" to jogo.genero,
        "desenvolvedora" to jogo.desenvolvedora,
        "anoLancamento" to jogo.anoLancamento,
        "resumo" to jogo.resumo,
        "precoEmDolar" to numeroOuNulo(jogo.precoEmDolar),
        "ofertas" to jogo.ofertas.map { oferta ->
            mapOf(
                "loja" to oferta.loja?.name,
                "nomeLoja" to oferta.nomeLoja,
                "plataforma" to oferta.plataforma,
                "preco" to numeroOuNulo(oferta.preco),
                "precoRegular" to numeroOuNulo(oferta.precoRegular),
                "url" to oferta.url
            )
        },
        "tags" to jogo.tags.toList(),
        CAMPO_ADICIONADO_EM to System.currentTimeMillis()
    )

    // Documentos sem título são ignorados (retorna null) para não quebrar a lista
    private fun paraJogo(documento: DocumentSnapshot): Jogo? {
        val titulo = documento.getString("titulo") ?: return null

        val ofertas = (documento.get("ofertas") as? List<*>).orEmpty()
            .filterIsInstance<Map<*, *>>()
            .map { mapa ->
                val loja = Loja.entries.find { it.name == mapa["loja"] }
                Oferta(
                    loja = loja,
                    preco = numeroOuNaN(mapa["preco"]),
                    nomeLoja = mapa["nomeLoja"] as? String ?: loja?.nome.orEmpty(),
                    plataforma = mapa["plataforma"] as? String ?: loja?.plataforma ?: "PC",
                    url = mapa["url"] as? String,
                    precoRegular = numeroOuNaN(mapa["precoRegular"])
                )
            }

        return Jogo(
            id = documento.getLong("id")?.toInt() ?: documento.id.hashCode(),
            titulo = titulo,
            capaUrl = documento.getString("capaUrl"),
            nota = numeroOuNaN(documento.get("nota")),
            precoLancamento = numeroOuNaN(documento.get("precoLancamento")),
            plataformas = textos(documento.get("plataformas"))
                .mapNotNull { nome -> Plataforma.entries.find { it.name == nome } },
            genero = documento.getString("genero").orEmpty(),
            desenvolvedora = documento.getString("desenvolvedora").orEmpty(),
            anoLancamento = documento.getLong("anoLancamento")?.toInt() ?: 0,
            resumo = documento.getString("resumo").orEmpty(),
            precoEmDolar = numeroOuNaN(documento.get("precoEmDolar")),
            ofertas = ofertas,
            idItad = documento.getString("idItad"),
            tags = textos(documento.get("tags")).toSet()
        )
    }

    private fun numeroOuNulo(valor: Double): Double? = valor.takeIf { it.isFinite() }

    private fun numeroOuNaN(valor: Any?): Double = (valor as? Number)?.toDouble() ?: Double.NaN

    private fun textos(valor: Any?): List<String> = (valor as? List<*>).orEmpty().filterIsInstance<String>()
}
