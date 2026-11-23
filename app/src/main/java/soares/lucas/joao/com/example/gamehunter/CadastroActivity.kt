package soares.lucas.joao.com.example.gamehunter

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityCadastroBinding
import java.net.HttpURLConnection
import java.net.URL

// Tela de CADASTRO de um novo usuário
class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private val autenticacao by lazy { FirebaseAuth.getInstance() }
    private val banco by lazy { FirebaseFirestore.getInstance() }

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
    // Integra o cadastro existente ao Firebase Authentication e ao Firestore.
    private fun realizarCadastro() {
        val nomeValido = validarCampoObrigatorio(binding.edtNome)
        val emailValido = validarCampoEmail(binding.edtEmail)
        val senhaValida = validarNovaSenha(binding.edtSenha, binding.edtConfirmarSenha, obrigatoria = true)
        val dataValida = validarCampoObrigatorio(binding.edtDataNascimento)

        if (!nomeValido || !emailValido || !senhaValida || !dataValida) {
            return
        }

        val nome = binding.edtNome.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val senha = binding.edtSenha.text.toString()
        val dataNascimento = binding.edtDataNascimento.text.toString()

        binding.btnCriarConta.isEnabled = false

        autenticacao.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user
                if (usuario == null) {
                    binding.btnCriarConta.isEnabled = true
                    Toast.makeText(this, R.string.erro_cadastro, Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val perfil = hashMapOf(
                    "nome" to nome,
                    "email" to email,
                    "dataNascimento" to dataNascimento
                )

                val foto = fotoSelecionada
                if (foto == null) {
                    salvarPerfilNoFirestore(usuario, perfil, nome, email, dataNascimento, "")
                } else {
                    lifecycleScope.launch {
                        val caminho = withContext(Dispatchers.IO) {
                            salvarFotoPerfilLocal(this@CadastroActivity, usuario.uid, foto)
                        }
                        if (caminho == null) {
                            cancelarCadastro(usuario, "", R.string.erro_enviar_foto)
                        } else {
                            salvarPerfilNoFirestore(
                                usuario,
                                perfil,
                                nome,
                                email,
                                dataNascimento,
                                caminho
                            )
                        }
                    }
                }
            }
            .addOnFailureListener {
                binding.btnCriarConta.isEnabled = true
                Toast.makeText(this, R.string.erro_cadastro, Toast.LENGTH_LONG).show()
            }
    }

    // Salva os dados do perfil depois que a foto opcional foi enviada.
    private fun salvarPerfilNoFirestore(
        usuario: FirebaseUser,
        perfil: Map<String, String>,
        nome: String,
        email: String,
        dataNascimento: String,
        caminhoFoto: String
    ) {
        banco.collection("usuarios")
            .document(usuario.uid)
            .set(perfil)
            .addOnSuccessListener {
                PreferenciasUsuario(this).apply {
                    salvarPerfil(nome, email, dataNascimento)
                    salvarFotoPerfil(usuario.uid, caminhoFoto)
                }
                enviarEmailBoasVindas(usuario, nome)
            }
            .addOnFailureListener {
                cancelarCadastro(usuario, caminhoFoto, R.string.erro_salvar_perfil)
            }
    }

    // Usa temporariamente o token da conta recém-criada para o Worker validar o
    // destinatário. O token não é salvo no dispositivo nem enviado ao Brevo.
    private fun enviarEmailBoasVindas(usuario: FirebaseUser, nome: String) {
        usuario.getIdToken(false)
            .addOnSuccessListener { resultado ->
                val token = resultado.token
                autenticacao.signOut()

                if (token.isNullOrBlank()) {
                    concluirCadastro(emailEnviado = false)
                    return@addOnSuccessListener
                }

                lifecycleScope.launch {
                    val enviado = withContext(Dispatchers.IO) {
                        solicitarEmailBoasVindas(token, nome)
                    }
                    concluirCadastro(emailEnviado = enviado)
                }
            }
            .addOnFailureListener {
                autenticacao.signOut()
                concluirCadastro(emailEnviado = false)
            }
    }

    // Chama somente o Worker protegido; a chave do Brevo permanece no Cloudflare.
    private fun solicitarEmailBoasVindas(token: String, nome: String): Boolean = runCatching {
        val conexao = URL(BuildConfig.WELCOME_EMAIL_URL).openConnection() as HttpURLConnection
        try {
            conexao.requestMethod = "POST"
            conexao.connectTimeout = 10_000
            conexao.readTimeout = 15_000
            conexao.doOutput = true
            conexao.setRequestProperty("Authorization", "Bearer $token")
            conexao.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val corpo = JSONObject().put("nome", nome).toString()
            conexao.outputStream.bufferedWriter(Charsets.UTF_8).use { escritor ->
                escritor.write(corpo)
            }

            conexao.responseCode in 200..299
        } finally {
            conexao.disconnect()
        }
    }.getOrDefault(false)

    // O cadastro permanece válido mesmo quando o serviço externo de e-mail falha.
    private fun concluirCadastro(emailEnviado: Boolean) {
        autenticacao.signOut()
        val mensagem = if (emailEnviado) {
            R.string.cadastro_sucesso
        } else {
            R.string.cadastro_sucesso_sem_email
        }
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        finish()
    }

    // Remove dados parciais quando não é possível concluir o cadastro.
    private fun cancelarCadastro(usuario: FirebaseUser, caminhoFoto: String, mensagem: Int) {
        removerFotoPerfilLocal(caminhoFoto)
        usuario.delete().addOnCompleteListener {
            autenticacao.signOut()
            binding.btnCriarConta.isEnabled = true
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        }
    }
}
