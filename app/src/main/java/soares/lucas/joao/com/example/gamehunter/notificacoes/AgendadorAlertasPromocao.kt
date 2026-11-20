package soares.lucas.joao.com.example.gamehunter.notificacoes

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import java.util.concurrent.TimeUnit

object AgendadorAlertasPromocao {
    private const val NOME_TRABALHO = "verificar_promocoes_favoritos"
    private const val INTERVALO_HORAS = 6L

    fun sincronizar(contexto: Context) {
        val workManager = WorkManager.getInstance(contexto)
        if (!PreferenciasUsuario(contexto).alertasPromocao) {
            workManager.cancelUniqueWork(NOME_TRABALHO)
            return
        }

        val restricoes = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val requisicao = PeriodicWorkRequestBuilder<VerificarPromocoesWorker>(
            INTERVALO_HORAS,
            TimeUnit.HOURS
        ).setConstraints(restricoes).build()

        workManager.enqueueUniquePeriodicWork(
            NOME_TRABALHO,
            ExistingPeriodicWorkPolicy.KEEP,
            requisicao
        )
    }
}
