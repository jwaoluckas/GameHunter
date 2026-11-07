package soares.lucas.joao.com.example.gamehunter

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityCadastroBinding

// Tela de CADASTRO de um novo usuário
class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding

    // Foto de perfil escolhida (opcional, por isso pode ser null)
    private var fotoSelecionada: Uri? = null

    // Abre a galeria e, quando o usuário volta com uma imagem, guarda o endereço
    // dela e a exibe dentro do círculo
    private val escolherFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoSelecionada = uri
            binding.imgFotoPerfil.setImageURI(uri)
            binding.imgFotoPerfil.visibility = View.VISIBLE
        }
    }

    // Cria a tela e liga o layout. O clipToOutline recorta a foto no formato
    // redondo do fundo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imgFotoPerfil.clipToOutline = true
        configurarCliques()
    }

    // Eventos de clique: voltar, foto, olhos das senhas, calendário, criar conta e "Entrar"
    private fun configurarCliques() {
        binding.btnVoltar.setOnClickListener { finish() }
        binding.linkEntrar.setOnClickListener { finish() }

        binding.frameFoto.setOnClickListener { escolherFoto.launch("image/*") }
        binding.txtAdicionarFoto.setOnClickListener { escolherFoto.launch("image/*") }

        binding.imgMostrarSenha.setOnClickListener {
            alternarVisibilidadeSenha(binding.edtSenha, binding.imgMostrarSenha)
        }
        binding.imgMostrarConfirmarSenha.setOnClickListener {
            alternarVisibilidadeSenha(binding.edtConfirmarSenha, binding.imgMostrarConfirmarSenha)
        }

        binding.edtDataNascimento.setOnClickListener { abrirCalendario(binding.edtDataNascimento) }
        binding.btnCriarConta.setOnClickListener { realizarCadastro() }
    }

    // Valida os campos (todos obrigatórios, e-mail válido, senha com no mínimo
    // TAMANHO_MINIMO_SENHA caracteres e confirmação igual). As validações são feitas
    // todas de uma vez para mostrar todos os erros juntos. Se estiver tudo certo,
    // salva o perfil nas preferências (exibido no menu de perfil e nas Configurações)
    // e volta para o Login
    private fun realizarCadastro() {
        val nomeValido = validarCampoObrigatorio(binding.edtNome)
        val emailValido = validarCampoEmail(binding.edtEmail)
        val senhaValida = validarNovaSenha(binding.edtSenha, binding.edtConfirmarSenha, obrigatoria = true)
        val dataValida = validarCampoObrigatorio(binding.edtDataNascimento)

        if (!nomeValido || !emailValido || !senhaValida || !dataValida) {
            return
        }

        // TODO (Backend): enviar nome, e-mail, senha, data de nascimento e a foto
        //  (fotoSelecionada, que pode ser null) para o serviço de cadastro da equipe.
        PreferenciasUsuario(this).salvarPerfil(
            nome = binding.edtNome.text.toString().trim(),
            email = binding.edtEmail.text.toString().trim(),
            dataNascimento = binding.edtDataNascimento.text.toString()
        )
        Toast.makeText(this, R.string.cadastro_sucesso, Toast.LENGTH_SHORT).show()
        finish()
    }
}
