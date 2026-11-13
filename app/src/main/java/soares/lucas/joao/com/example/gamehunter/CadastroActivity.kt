package soares.lucas.joao.com.example.gamehunter

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityCadastroBinding

// Tela de CADASTRO de um novo usuário
class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private val autenticacao by lazy { FirebaseAuth.getInstance() }
    private val banco by lazy { FirebaseFirestore.getInstance() }
    private val armazenamento by lazy { FirebaseStorage.getInstance() }

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
                    val caminho = caminhoFotoPerfil(usuario.uid)
                    val metadata = StorageMetadata.Builder()
                        .setContentType(contentResolver.getType(foto) ?: "image/jpeg")
                        .build()
                    armazenamento.reference.child(caminho)
                        .putFile(foto, metadata)
                        .addOnSuccessListener {
                            perfil["fotoCaminho"] = caminho
                            salvarPerfilNoFirestore(
                                usuario,
                                perfil,
                                nome,
                                email,
                                dataNascimento,
                                caminho
                            )
                        }
                        .addOnFailureListener {
                            cancelarCadastro(usuario, "", R.string.erro_enviar_foto)
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
                    salvarFotoPerfil(caminhoFoto)
                }
                autenticacao.signOut()
                Toast.makeText(this, R.string.cadastro_sucesso, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                cancelarCadastro(usuario, caminhoFoto, R.string.erro_salvar_perfil)
            }
    }

    // Remove dados parciais quando não é possível concluir o cadastro.
    private fun cancelarCadastro(usuario: FirebaseUser, caminhoFoto: String, mensagem: Int) {
        val removerUsuario = {
            usuario.delete().addOnCompleteListener {
                autenticacao.signOut()
                binding.btnCriarConta.isEnabled = true
                Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
            }
        }

        if (caminhoFoto.isEmpty()) {
            removerUsuario()
        } else {
            armazenamento.reference.child(caminhoFoto).delete().addOnCompleteListener {
                removerUsuario()
            }
        }
    }
}
