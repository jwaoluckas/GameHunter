package soares.lucas.joao.com.example.gamehunter.dados

import android.content.Context
import androidx.core.content.edit
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Dados do usuário salvos no próprio aparelho com SharedPreferences (pares chave-valor
// que continuam salvos mesmo depois de fechar o app). São dois tipos de dado:
// - PREFERÊNCIAS DE TELA (moeda padrão, alertas de promoção e moedas escolhidas no
//   modal "Comparar moedas"): ficam no aparelho mesmo
// - PERFIL (nome, e-mail e data de nascimento): é PROVISÓRIO, só para o menu de perfil
//   e as Configurações funcionarem sem o servidor.
// TODO (Backend): o perfil deve vir da conta do usuário no servidor (RNF03); quando
//  existir, Login, Cadastro e Configurações passam a usar o Backend e as funções
//  salvarEmail/salvarPerfil (e as leituras de nome, email e dataNascimento) saem daqui
class PreferenciasUsuario(contexto: Context) {

    // Nome do arquivo, chaves e valores padrão das preferências
    companion object {
        private const val NOME_ARQUIVO = "preferencias_gamehunter"
        private const val CHAVE_NOME = "nome"
        private const val CHAVE_EMAIL = "email"
        private const val CHAVE_DATA_NASCIMENTO = "data_nascimento"
        private const val CHAVE_FOTO_CAMINHO = "foto_caminho"
        private const val CHAVE_MOEDA_PADRAO = "moeda_padrao"
        private const val CHAVE_ALERTAS_PROMOCAO = "alertas_promocao"
        private const val CHAVE_MOEDAS_SELECIONADAS = "moedas_selecionadas"

        private val MOEDA_PADRAO = Moeda.BRL
        private val MOEDAS_SELECIONADAS_PADRAO = listOf(Moeda.USD, Moeda.BRL, Moeda.EUR, Moeda.GBP)
    }

    private val preferences = contexto.getSharedPreferences(NOME_ARQUIVO, Context.MODE_PRIVATE)

    // Leitura das preferências. As moedas são salvas como texto pelo código
    // (ex.: "BRL" ou "USD,BRL,EUR") e convertidas de volta para o enum Moeda;
    // códigos desconhecidos são ignorados e, se nada sobrar, usa-se o valor padrão
    val nome: String
        get() = preferences.getString(CHAVE_NOME, "").orEmpty()

    val email: String
        get() = preferences.getString(CHAVE_EMAIL, "").orEmpty()

    val dataNascimento: String
        get() = preferences.getString(CHAVE_DATA_NASCIMENTO, "").orEmpty()

    val fotoCaminho: String
        get() = preferences.getString(CHAVE_FOTO_CAMINHO, "").orEmpty()

    val alertasPromocao: Boolean
        get() = preferences.getBoolean(CHAVE_ALERTAS_PROMOCAO, true)

    val moedaPadrao: Moeda
        get() {
            val codigo = preferences.getString(CHAVE_MOEDA_PADRAO, MOEDA_PADRAO.name)
            return Moeda.entries.find { it.name == codigo } ?: MOEDA_PADRAO
        }

    val moedasSelecionadas: List<Moeda>
        get() {
            val codigos = preferences.getString(CHAVE_MOEDAS_SELECIONADAS, "").orEmpty().split(",")
            val moedas = codigos.mapNotNull { codigo -> Moeda.entries.find { it.name == codigo } }
            return moedas.ifEmpty { MOEDAS_SELECIONADAS_PADRAO }
        }

    // Gravação das preferências. Cada função salva um grupo de dados de uma vez,
    // dentro de um único edit { }
    fun salvarEmail(email: String) {
        preferences.edit { putString(CHAVE_EMAIL, email) }
    }

    fun salvarPerfil(nome: String, email: String, dataNascimento: String) {
        preferences.edit {
            putString(CHAVE_NOME, nome)
            putString(CHAVE_EMAIL, email)
            putString(CHAVE_DATA_NASCIMENTO, dataNascimento)
        }
    }

    // Mantém localmente o caminho da foto privada salva no Firebase Storage.
    fun salvarFotoPerfil(caminho: String) {
        preferences.edit { putString(CHAVE_FOTO_CAMINHO, caminho) }
    }

    fun salvarPreferencias(moedaPadrao: Moeda, alertasPromocao: Boolean) {
        preferences.edit {
            putString(CHAVE_MOEDA_PADRAO, moedaPadrao.name)
            putBoolean(CHAVE_ALERTAS_PROMOCAO, alertasPromocao)
        }
    }

    fun salvarMoedasSelecionadas(moedas: List<Moeda>) {
        preferences.edit {
            putString(CHAVE_MOEDAS_SELECIONADAS, moedas.joinToString(",") { it.name })
        }
    }
}
