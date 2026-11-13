package soares.lucas.joao.com.example.gamehunter

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import soares.lucas.joao.com.example.gamehunter.componentes.GrupoDeChips
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityConfiguracoesBinding
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Tela de CONFIGURAÇÕES, aberta pelo menu de perfil. Permite alterar a foto, os
// dados pessoais, a senha, a moeda padrão de comparação e os alertas de promoção.
// Os dados ficam salvos com SharedPreferences (classe PreferenciasUsuario)
class ConfiguracoesActivity : AppCompatActivity() {

    // Moedas oferecidas como padrão nas Configurações, como no Figma
    companion object {
        private val MOEDAS_PADRAO = listOf(Moeda.BRL, Moeda.USD, Moeda.EUR)
    }

    private lateinit var binding: ActivityConfiguracoesBinding
    private lateinit var preferencias: PreferenciasUsuario
    private lateinit var grupoMoedaPadrao: GrupoDeChips
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

    // Cria a tela e segue três passos: monta os chips de moeda, carrega o que já
    // estava salvo e configura os cliques. O clipToOutline recorta a foto no
    // formato redondo do fundo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracoesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencias = PreferenciasUsuario(this)
        binding.imgFotoPerfil.clipToOutline = true

        configurarMoedaPadrao()
        carregarPreferencias()
        configurarEventos()
    }

    // Chips "BRL – Real", "USD – Dólar" e "EUR – Euro", com uma única opção marcada
    private fun configurarMoedaPadrao() {
        grupoMoedaPadrao = GrupoDeChips(
            binding.flowMoedaPadrao,
            MOEDAS_PADRAO.map { getString(R.string.config_formato_moeda, it.name, it.nome) },
            selecaoMultipla = false
        )
    }

    // Preenche a tela com os dados salvos. Se a moeda salva não estiver entre as
    // opções da tela, marca a primeira (BRL)
    private fun carregarPreferencias() {
        binding.edtNome.setText(preferencias.nome)
        binding.edtEmail.setText(preferencias.email)
        binding.edtDataNascimento.setText(preferencias.dataNascimento)
        binding.swAlertasPromocao.isChecked = preferencias.alertasPromocao
        carregarFotoPerfil(binding.imgFotoPerfil, preferencias.fotoCaminho)

        val indiceMoeda = MOEDAS_PADRAO.indexOf(preferencias.moedaPadrao)
        grupoMoedaPadrao.selecionar(if (indiceMoeda >= 0) indiceMoeda else 0)
    }

    // Eventos de clique: voltar, foto, olhos das senhas, calendário e salvar
    private fun configurarEventos() {
        binding.btnVoltar.setOnClickListener { finish() }

        binding.frameFoto.setOnClickListener { escolherFoto.launch("image/*") }
        binding.txtAlterarFoto.setOnClickListener { escolherFoto.launch("image/*") }

        binding.imgMostrarNovaSenha.setOnClickListener {
            alternarVisibilidadeSenha(binding.edtNovaSenha, binding.imgMostrarNovaSenha)
        }
        binding.imgMostrarConfirmarSenha.setOnClickListener {
            alternarVisibilidadeSenha(binding.edtConfirmarSenha, binding.imgMostrarConfirmarSenha)
        }

        binding.edtDataNascimento.setOnClickListener { abrirCalendario(binding.edtDataNascimento) }
        binding.btnSalvar.setOnClickListener { salvarAlteracoes() }
    }

    // Valida os campos (nome, e-mail e data obrigatórios; a senha só é validada se o
    // usuário digitou alguma coisa) e, se estiver tudo certo, salva e volta.
    // As validações são feitas todas de uma vez para mostrar todos os erros juntos
    private fun salvarAlteracoes() {
        val nomeValido = validarCampoObrigatorio(binding.edtNome)
        val emailValido = validarCampoEmail(binding.edtEmail)
        val dataValida = validarCampoObrigatorio(binding.edtDataNascimento)
        val senhaValida = validarNovaSenha(binding.edtNovaSenha, binding.edtConfirmarSenha, obrigatoria = false)

        if (!nomeValido || !emailValido || !dataValida || !senhaValida) {
            return
        }

        preferencias.salvarPerfil(
            nome = binding.edtNome.text.toString().trim(),
            email = binding.edtEmail.text.toString().trim(),
            dataNascimento = binding.edtDataNascimento.text.toString()
        )
        preferencias.salvarPreferencias(
            moedaPadrao = MOEDAS_PADRAO[grupoMoedaPadrao.selecionado()],
            alertasPromocao = binding.swAlertasPromocao.isChecked
        )

        // TODO (Backend): sincronizar nome, e-mail, data e nova senha com a conta.
        val foto = fotoSelecionada
        if (foto != null) {
            enviarFotoPerfil(foto)
            return
        }

        //  TODO (Notificações): usar alertasPromocao para avisar o usuário sobre promoções
        concluirSalvamento()
    }

    // Envia a nova foto e associa seu caminho ao perfil no Firestore.
    private fun enviarFotoPerfil(foto: Uri) {
        val usuario = autenticacao.currentUser
        if (usuario == null) {
            Toast.makeText(this, R.string.erro_sessao_expirada, Toast.LENGTH_LONG).show()
            return
        }

        binding.btnSalvar.isEnabled = false
        val caminho = caminhoFotoPerfil(usuario.uid)
        val metadata = StorageMetadata.Builder()
            .setContentType(contentResolver.getType(foto) ?: "image/jpeg")
            .build()

        armazenamento.reference.child(caminho)
            .putFile(foto, metadata)
            .addOnSuccessListener {
                banco.collection("usuarios")
                    .document(usuario.uid)
                    .set(mapOf("fotoCaminho" to caminho), SetOptions.merge())
                    .addOnSuccessListener {
                        preferencias.salvarFotoPerfil(caminho)
                        concluirSalvamento()
                    }
                    .addOnFailureListener {
                        binding.btnSalvar.isEnabled = true
                        Toast.makeText(this, R.string.erro_salvar_foto, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                binding.btnSalvar.isEnabled = true
                Toast.makeText(this, R.string.erro_enviar_foto, Toast.LENGTH_LONG).show()
            }
    }

    // Confirma as alterações mantendo o comportamento original da tela.
    private fun concluirSalvamento() {
        Toast.makeText(this, R.string.config_salvo, Toast.LENGTH_SHORT).show()
        finish()
    }
}
