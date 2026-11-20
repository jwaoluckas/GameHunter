package soares.lucas.joao.com.example.gamehunter.notificacoes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import soares.lucas.joao.com.example.gamehunter.FavoritosActivity
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.formatarValorNaMoeda
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

object NotificacoesPromocao {
    private const val ID_CANAL = "promocoes_favoritos"

    fun criarCanal(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            ID_CANAL,
            contexto.getString(R.string.alerta_canal_nome),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = contexto.getString(R.string.alerta_canal_descricao)
        }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun exibir(
        contexto: Context,
        uid: String,
        idItad: String,
        titulo: String,
        precoMedio: Double,
        desconto: Int
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        val gerenciador = NotificationManagerCompat.from(contexto)
        if (!gerenciador.areNotificationsEnabled()) return false

        val idNotificacao = "$uid:$idItad".hashCode()
        val abrirFavoritos = Intent(contexto, FavoritosActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val acao = PendingIntent.getActivity(
            contexto,
            idNotificacao,
            abrirFavoritos,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val preco = formatarValorNaMoeda(contexto, Moeda.BRL, precoMedio)
        val notificacao = NotificationCompat.Builder(contexto, ID_CANAL)
            .setSmallIcon(R.drawable.ic_estrela_preenchida)
            .setContentTitle(contexto.getString(R.string.alerta_promocao_titulo, titulo))
            .setContentText(contexto.getString(R.string.alerta_promocao_texto, preco, desconto))
            .setContentIntent(acao)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        gerenciador.notify(idNotificacao, notificacao)
        return true
    }
}
