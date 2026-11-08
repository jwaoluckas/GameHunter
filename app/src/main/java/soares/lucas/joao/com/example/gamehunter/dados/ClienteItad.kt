package soares.lucas.joao.com.example.gamehunter.dados

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// A chave vai no cabeçalho, nunca na URL ou em logs. Não seguimos redirecionamentos
// da API para evitar enviar esse cabeçalho para outro servidor.
internal class ClienteItad(private val chave: String) {
    suspend fun consultar(caminho: String, corpo: String? = null): String {
        if (chave.isBlank()) throw ErroItad(ErroItad.Tipo.SEM_CHAVE)
        return runInterruptible(Dispatchers.IO) {
            val conexao = URL("https://api.isthereanydeal.com$caminho").openConnection() as HttpsURLConnection
            try {
                conexao.connectTimeout = 15_000
                conexao.readTimeout = 20_000
                conexao.instanceFollowRedirects = false
                conexao.setRequestProperty("ITAD-API-Key", chave)
                conexao.setRequestProperty("Accept", "application/json")
                if (corpo != null) {
                    conexao.requestMethod = "POST"
                    conexao.doOutput = true
                    conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conexao.outputStream.use { it.write(corpo.toByteArray(Charsets.UTF_8)) }
                }
                when (conexao.responseCode) {
                    in 200..299 -> conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    401, 403 -> throw ErroItad(ErroItad.Tipo.CHAVE_INVALIDA)
                    404 -> throw ErroItad(ErroItad.Tipo.NAO_ENCONTRADO)
                    429 -> throw ErroItad(ErroItad.Tipo.LIMITE)
                    else -> throw ErroItad(ErroItad.Tipo.SERVIDOR)
                }
            } finally {
                conexao.disconnect()
            }
        }
    }
}

internal class ErroItad(val tipo: Tipo) : IOException(tipo.name) {
    enum class Tipo { SEM_CHAVE, CHAVE_INVALIDA, NAO_ENCONTRADO, LIMITE, SERVIDOR }
}
