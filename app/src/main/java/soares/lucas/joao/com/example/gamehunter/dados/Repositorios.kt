package soares.lucas.joao.com.example.gamehunter.dados

import android.content.Context
import soares.lucas.joao.com.example.gamehunter.BuildConfig
import soares.lucas.joao.com.example.gamehunter.R

// Uma instância compartilhada preserva o cache entre a tela inicial e a pesquisa.
// Não retém a Activity: somente os textos localizados são passados ao repositório.
object Repositorios {
    private var instancia: RepositorioDeJogos? = null

    @Synchronized
    fun jogos(contexto: Context): RepositorioDeJogos = instancia ?: RepositorioItad(
        ClienteItad(BuildConfig.ITAD_API_KEY)::consultar,
        contexto.getString(R.string.itad_ofertas),
        contexto.getString(R.string.itad_descontos),
        contexto.getString(R.string.itad_fonte)
    ).also { instancia = it }
}
