package soares.lucas.joao.com.example.gamehunter.notificacoes

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import soares.lucas.joao.com.example.gamehunter.BuildConfig
import soares.lucas.joao.com.example.gamehunter.dados.ClienteItad
import soares.lucas.joao.com.example.gamehunter.dados.ErroItad
import soares.lucas.joao.com.example.gamehunter.dados.LeitorPromocoesItad
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.dados.PromocaoAtual
import soares.lucas.joao.com.example.gamehunter.dados.RepositorioFavoritos
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import java.io.IOException

class VerificarPromocoesWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        if (!PreferenciasUsuario(applicationContext).alertasPromocao) return Result.success()
        val usuario = FirebaseAuth.getInstance().currentUser ?: return Result.success()

        val favoritos = try {
            RepositorioFavoritos().listarParaAlertas().filter { it.idItad != null }
        } catch (erro: CancellationException) {
            throw erro
        } catch (_: Exception) {
            return Result.retry()
        }
        if (favoritos.isEmpty()) return Result.success()

        val promocoes = try {
            consultarPromocoes(favoritos.mapNotNull { it.idItad })
        } catch (erro: CancellationException) {
            throw erro
        } catch (erro: ErroItad) {
            return if (erro.tipo == ErroItad.Tipo.SEM_CHAVE || erro.tipo == ErroItad.Tipo.CHAVE_INVALIDA) {
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (_: IOException) {
            return Result.retry()
        } catch (_: Exception) {
            return Result.failure()
        }

        val estados = EstadosAlertas(applicationContext)
        favoritos.forEach { jogo ->
            val id = jogo.idItad ?: return@forEach
            val atual = promocoes[id] ?: return@forEach
            val anterior = estados.ler(usuario.uid, id) ?: jogo.emPromocao
            val entrouEmPromocao = !anterior && atual.emPromocao
            val notificada = !entrouEmPromocao || NotificacoesPromocao.exibir(
                applicationContext,
                usuario.uid,
                id,
                jogo.titulo,
                atual.precoMedio,
                atual.desconto
            )
            if (notificada) estados.salvar(usuario.uid, id, atual.emPromocao)
        }
        return Result.success()
    }

    private suspend fun consultarPromocoes(ids: List<String>): Map<String, PromocaoAtual> {
        val cliente = ClienteItad(BuildConfig.ITAD_API_KEY)
        return ids.distinct().chunked(50).flatMap { lote ->
            val resposta = cliente.consultar(
                "/games/prices/v3?country=BR&vouchers=false",
                JSONArray(lote).toString()
            )
            LeitorPromocoesItad.ler(resposta).values
        }.associateBy { it.idItad }
    }
}

private class EstadosAlertas(contexto: Context) {
    private val preferencias = contexto.getSharedPreferences("estados_alertas_promocao", Context.MODE_PRIVATE)

    fun ler(uid: String, idItad: String): Boolean? {
        val chave = chave(uid, idItad)
        return if (preferencias.contains(chave)) preferencias.getBoolean(chave, false) else null
    }

    fun salvar(uid: String, idItad: String, emPromocao: Boolean) {
        preferencias.edit { putBoolean(chave(uid, idItad), emPromocao) }
    }

    private fun chave(uid: String, idItad: String): String = "$uid:$idItad"
}
