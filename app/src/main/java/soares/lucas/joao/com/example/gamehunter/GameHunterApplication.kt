package soares.lucas.joao.com.example.gamehunter

import android.app.Application
import soares.lucas.joao.com.example.gamehunter.notificacoes.AgendadorAlertasPromocao
import soares.lucas.joao.com.example.gamehunter.notificacoes.NotificacoesPromocao

class GameHunterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificacoesPromocao.criarCanal(this)
        AgendadorAlertasPromocao.sincronizar(this)
    }
}
