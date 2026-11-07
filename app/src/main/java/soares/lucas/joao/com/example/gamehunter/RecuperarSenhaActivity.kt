package soares.lucas.joao.com.example.gamehunter

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityRecuperarSenhaBinding

// Tela de RECUPERAR SENHA
class RecuperarSenhaActivity : AppCompatActivity() {

    // Chave do extra com o e-mail enviado pela tela de Login
    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var binding: ActivityRecuperarSenhaBinding

    // Cria a tela, preenche o e-mail recebido do Login (se houver) e configura os cliques
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecuperarSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.edtEmail.setText(intent.getStringExtra(EXTRA_EMAIL).orEmpty())

        binding.btnVoltar.setOnClickListener { finish() }
        binding.linkVoltarLogin.setOnClickListener { finish() }
        binding.btnEnviar.setOnClickListener { enviarLinkRecuperacao() }
    }

    // Valida o e-mail (vazio ou com formato inválido mostra erro no campo)
    // e, se estiver certo, confirma o envio
    private fun enviarLinkRecuperacao() {
        if (!validarCampoEmail(binding.edtEmail)) {
            return
        }

        // TODO (Backend): enviar o e-mail para o serviço de recuperação de senha da equipe.
        mostrarDialogoConfirmacao(binding.edtEmail.text.toString().trim())
    }

    // AlertDialog avisando que o link foi enviado. Não pode ser fechado tocando fora;
    // o "OK" volta para o Login
    private fun mostrarDialogoConfirmacao(email: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.recuperar_dialogo_titulo)
            .setMessage(getString(R.string.recuperar_dialogo_mensagem, email))
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .show()
    }
}
