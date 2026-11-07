package soares.lucas.joao.com.example.gamehunter

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityMainBinding

// Tela de LOGIN: primeira tela aberta pelo app (definido no AndroidManifest.xml)
class MainActivity : AppCompatActivity() {

    // Acesso às Views do layout activity_main.xml via View Binding
    private lateinit var binding: ActivityMainBinding

    // Cria a tela, liga o layout e configura os cliques e o botão de voltar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarCliques()
        configurarBotaoVoltar()
    }

    // Eventos de clique: olho da senha, Entrar, Cadastrar e "Esqueci minha senha".
    // A navegação usa Intents explícitas; o e-mail digitado vai como extra para a
    // tela de recuperar senha já abrir preenchida
    private fun configurarCliques() {
        binding.imgMostrarSenha.setOnClickListener {
            alternarVisibilidadeSenha(binding.edtSenha, binding.imgMostrarSenha)
        }

        binding.btnEntrar.setOnClickListener { realizarLogin() }

        binding.btnCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }

        binding.txtEsqueciSenha.setOnClickListener {
            val intent = Intent(this, RecuperarSenhaActivity::class.java)
            intent.putExtra(RecuperarSenhaActivity.EXTRA_EMAIL, binding.edtEmail.text.toString().trim())
            startActivity(intent)
        }
    }

    // Valida os campos (e-mail obrigatório e válido, senha obrigatória), salva o e-mail
    // nas preferências (exibido no menu de perfil) e abre a tela Principal.
    // O Login é fechado com finish() para que o botão de voltar na Principal não
    // retorne para cá
    private fun realizarLogin() {
        val emailValido = validarCampoEmail(binding.edtEmail)
        val senhaValida = validarCampoObrigatorio(binding.edtSenha)

        if (!emailValido || !senhaValida) {
            return
        }

        val email = binding.edtEmail.text.toString().trim()

        // TODO (Backend): autenticar e-mail e senha no serviço da equipe
        //  e só abrir a tela Principal se o login for aceito.
        PreferenciasUsuario(this).salvarEmail(email)
        Toast.makeText(this, getString(R.string.login_sucesso, email), Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, PrincipalActivity::class.java))
        finish()
    }

    // Ao apertar voltar, pergunta com um AlertDialog se o usuário quer mesmo sair
    private fun configurarBotaoVoltar() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.sair_titulo)
                    .setMessage(R.string.sair_mensagem)
                    .setPositiveButton(R.string.sim) { _, _ -> finish() }
                    .setNegativeButton(R.string.nao, null)
                    .show()
            }
        })
    }
}
