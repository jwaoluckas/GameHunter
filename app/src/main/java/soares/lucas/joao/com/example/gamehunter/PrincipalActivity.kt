package soares.lucas.joao.com.example.gamehunter

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityPrincipalBinding
import soares.lucas.joao.com.example.gamehunter.databinding.DialogMenuPerfilBinding

// Tela PRINCIPAL, aberta depois do login. Mostra o header (logo + perfil) e hospeda
// o InicioFragment, que tem o conteúdo da tela
class PrincipalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrincipalBinding

    // Cria a tela e coloca o InicioFragment no container com o supportFragmentManager.
    // O "if" evita adicionar o Fragment de novo quando a tela é recriada (ex.: girar o
    // celular), pois nesse caso o Android já o restaura sozinho
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.containerFragment, InicioFragment())
                .commit()
        }

        binding.btnPerfil.setOnClickListener { mostrarMenuPerfil() }
        configurarBotaoVoltar()
    }

    // Menu do perfil (Configurações e Sair), feito com um AlertDialog usando o layout
    // dialog_menu_perfil.xml. Nome e e-mail vêm das preferências salvas no Login,
    // Cadastro e Configurações; sem nome salvo, aparece "Jogador"
    private fun mostrarMenuPerfil() {
        val menuBinding = DialogMenuPerfilBinding.inflate(layoutInflater)
        val preferencias = PreferenciasUsuario(this)

        menuBinding.txtNomeUsuario.text = preferencias.nome.ifEmpty { getString(R.string.usuario_padrao) }
        menuBinding.txtEmailUsuario.text = preferencias.email
        menuBinding.imgFotoUsuario.clipToOutline = true
        carregarFotoPerfil(menuBinding.imgFotoUsuario, preferencias.fotoCaminho)

        val dialogo = AlertDialog.Builder(this)
            .setView(menuBinding.root)
            .create()

        menuBinding.itemConfiguracoes.setOnClickListener {
            dialogo.dismiss()
            startActivity(Intent(this, ConfiguracoesActivity::class.java))
        }

        menuBinding.itemSair.setOnClickListener {
            dialogo.dismiss()
            sair()
        }

        dialogo.show()

        // Ajusta a janela do diálogo para ficar como no Figma: fundo transparente
        // (aparece só o bg_menu_perfil), no canto superior direito, logo abaixo do header,
        // com 232dp de largura. Os valores em dp são convertidos para pixels pela densidade da tela
        dialogo.window?.let { janela ->
            janela.setBackgroundDrawableResource(android.R.color.transparent)
            janela.setGravity(Gravity.TOP or Gravity.END)

            val densidade = resources.displayMetrics.density
            val atributos = janela.attributes
            atributos.x = (16 * densidade).toInt()
            atributos.y = (64 * densidade).toInt()
            atributos.width = (232 * densidade).toInt()
            janela.attributes = atributos
        }
    }

    // Volta para o Login limpando a pilha de telas (as flags fazem com que o botão
    // de voltar no Login não retorne para a Principal)
    // Encerra a sessão do Firebase antes de voltar para o Login.
    private fun sair() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Ao apertar voltar, pergunta com um AlertDialog se o usuário quer mesmo sair
    private fun configurarBotaoVoltar() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@PrincipalActivity)
                    .setTitle(R.string.sair_titulo)
                    .setMessage(R.string.sair_mensagem)
                    .setPositiveButton(R.string.sim) { _, _ -> finish() }
                    .setNegativeButton(R.string.nao, null)
                    .show()
            }
        })
    }
}
