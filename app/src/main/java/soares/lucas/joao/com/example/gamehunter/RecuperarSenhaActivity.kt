package soares.lucas.joao.com.example.gamehunter

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityRecuperarSenhaBinding

// Tela de RECUPERAR SENHA
class RecuperarSenhaActivity : AppCompatActivity() {

    // Chave do extra com o e-mail enviado pela tela de Login
    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var binding: ActivityRecuperarSenhaBinding
    private val autenticacao by lazy { FirebaseAuth.getInstance() }

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
    // Solicita ao Firebase o envio do link para redefinir a senha.
    private fun enviarLinkRecuperacao() {
        if (!validarCampoEmail(binding.edtEmail)) {
            return
        }

        val email = binding.edtEmail.text.toString().trim()
        binding.btnEnviar.isEnabled = false
        autenticacao.setLanguageCode("pt-BR")

        autenticacao.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                binding.btnEnviar.isEnabled = true
                mostrarDialogoConfirmacao(email)
            }
            .addOnFailureListener {
                binding.btnEnviar.isEnabled = true
                Toast.makeText(this, R.string.erro_recuperar_senha, Toast.LENGTH_LONG).show()
            }
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
