package soares.lucas.joao.com.example.gamehunter.modais

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.formatarValorNaMoeda
import soares.lucas.joao.com.example.gamehunter.modelo.EstadoPrecoRegional
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Os modais recebem o contexto das Activities. O escopo termina tanto com a
// Activity quanto quando o diálogo é dispensado, sem cancelar a tela inteira.
internal fun Context.criarEscopoDoModal(): CoroutineScope {
    val pai = (this as LifecycleOwner).lifecycleScope.coroutineContext
    return CoroutineScope(pai + SupervisorJob(pai[Job]))
}

internal fun textoPrecoRegional(contexto: Context, moeda: Moeda, estado: EstadoPrecoRegional?): String =
    when (estado) {
        is EstadoPrecoRegional.Disponivel -> formatarValorNaMoeda(contexto, moeda, estado.valor)
        is EstadoPrecoRegional.Falha -> contexto.getString(R.string.moedas_erro_preco)
        EstadoPrecoRegional.SemOfertas -> contexto.getString(R.string.moedas_sem_oferta)
        else -> contexto.getString(R.string.moedas_carregando)
    }
