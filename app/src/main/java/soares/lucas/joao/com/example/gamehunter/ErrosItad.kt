package soares.lucas.joao.com.example.gamehunter

import soares.lucas.joao.com.example.gamehunter.dados.ErroItad
import java.io.IOException

// Mensagens controladas: nunca exibimos a resposta HTTP ou dados da chave na tela.
internal fun mensagemErroItad(erro: Exception): Int = when (erro) {
    is ErroItad -> when (erro.tipo) {
        ErroItad.Tipo.SEM_CHAVE -> R.string.itad_sem_chave
        ErroItad.Tipo.CHAVE_INVALIDA -> R.string.itad_chave_invalida
        ErroItad.Tipo.LIMITE -> R.string.itad_limite
        else -> R.string.itad_erro
    }
    is IOException -> R.string.itad_erro_rede
    else -> R.string.itad_erro
}
