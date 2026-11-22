package soares.lucas.joao.com.example.gamehunter

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import soares.lucas.joao.com.example.gamehunter.componentes.GrupoDeChips
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityConfiguracoesBinding
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda
import soares.lucas.joao.com.example.gamehunter.notificacoes.AgendadorAlertasPromocao

// Tela de CONFIGURAÇÕES, aberta pelo menu de perfil. Permite alterar a foto, os
// dados pessoais, a senha, a moeda padrão de comparação e os alertas de promoção.
// Dados da conta sincronizam com Firebase; foto e preferências do aparelho ficam locais.
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
    private val solicitarPermissaoNotificacoes =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            if (!concedida && ::binding.isInitialized) {
                binding.swAlertasPromocao.isChecked = false
                Toast.makeText(this, R.string.alerta_permissao_negada, Toast.LENGTH_SHORT).show()
            }
        }

    // Foto de perfil escolhida (opcional, por isso pode ser null)
    private var fotoSelecionada: Uri? = null
    private var emailAguardandoConfirmacao = false

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
        autenticacao.currentUser?.uid?.let { uid ->
            carregarFotoPerfil(binding.imgFotoPerfil, preferencias.fotoCaminho(uid))
        }

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
        binding.swAlertasPromocao.setOnCheckedChangeListener { _, marcado ->
            if (marcado) solicitarPermissaoParaAlertas()
        }
        binding.btnSalvar.setOnClickListener { salvarAlteracoes() }
    }

    private fun solicitarPermissaoParaAlertas() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            solicitarPermissaoNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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

        val usuario = autenticacao.currentUser
        if (usuario == null) {
            Toast.makeText(this, R.string.erro_sessao_expirada, Toast.LENGTH_LONG).show()
            return
        }

        val nome = binding.edtNome.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val dataNascimento = binding.edtDataNascimento.text.toString()
        val novaSenha = binding.edtNovaSenha.text.toString()
        binding.btnSalvar.isEnabled = false

        atualizarSenhaSeNecessario(usuario, novaSenha) {
            atualizarEmailSeNecessario(usuario, email) {
                salvarPerfilRemoto(usuario, nome, dataNascimento)
            }
        }
    }

    // Senha pertence ao Firebase Authentication e so e alterada quando preenchida.
    private fun atualizarSenhaSeNecessario(
        usuario: FirebaseUser,
        novaSenha: String,
        aoConcluir: () -> Unit
    ) {
        if (novaSenha.isBlank()) return aoConcluir()
        usuario.updatePassword(novaSenha)
            .addOnSuccessListener { aoConcluir() }
            .addOnFailureListener(::tratarFalhaCredencial)
    }

    // O Firebase envia uma verificacao ao novo endereco antes de efetivar a troca.
    private fun atualizarEmailSeNecessario(
        usuario: FirebaseUser,
        novoEmail: String,
        aoConcluir: () -> Unit
    ) {
        if (novoEmail.equals(usuario.email, ignoreCase = true)) return aoConcluir()
        autenticacao.setLanguageCode("pt-BR")
        usuario.verifyBeforeUpdateEmail(novoEmail)
            .addOnSuccessListener {
                emailAguardandoConfirmacao = true
                aoConcluir()
            }
            .addOnFailureListener(::tratarFalhaCredencial)
    }

    // Nome, nascimento e o e-mail ja confirmado permanecem no documento do perfil.
    private fun salvarPerfilRemoto(
        usuario: FirebaseUser,
        nome: String,
        dataNascimento: String
    ) {
        val emailConfirmado = usuario.email.orEmpty().ifEmpty { preferencias.email }
        banco.collection("usuarios")
            .document(usuario.uid)
            .set(
                mapOf(
                    "nome" to nome,
                    "email" to emailConfirmado,
                    "dataNascimento" to dataNascimento
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                salvarPreferenciasLocais(nome, emailConfirmado, dataNascimento)
                val foto = fotoSelecionada
                if (foto == null) concluirSalvamento() else enviarFotoPerfil(foto)
            }
            .addOnFailureListener {
                binding.btnSalvar.isEnabled = true
                Toast.makeText(this, R.string.erro_atualizar_perfil, Toast.LENGTH_LONG).show()
            }
    }

    private fun tratarFalhaCredencial(erro: Exception) {
        binding.btnSalvar.isEnabled = true
        val mensagem = if (erro is FirebaseAuthRecentLoginRequiredException) {
            R.string.erro_login_recente
        } else {
            R.string.erro_atualizar_credenciais
        }
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }

    private fun salvarPreferenciasLocais(nome: String, email: String, dataNascimento: String) {
        preferencias.salvarPerfil(
            nome = nome,
            email = email,
            dataNascimento = dataNascimento
        )
        preferencias.salvarPreferencias(
            moedaPadrao = MOEDAS_PADRAO[grupoMoedaPadrao.selecionado()],
            alertasPromocao = binding.swAlertasPromocao.isChecked
        )
        AgendadorAlertasPromocao.sincronizar(this)
    }

    // Reduz e salva a nova foto somente na pasta privada deste dispositivo.
    private fun enviarFotoPerfil(foto: Uri) {
        val usuario = autenticacao.currentUser
        if (usuario == null) {
            Toast.makeText(this, R.string.erro_sessao_expirada, Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            val caminho = withContext(Dispatchers.IO) {
                salvarFotoPerfilLocal(this@ConfiguracoesActivity, usuario.uid, foto)
            }
            if (caminho == null) {
                binding.btnSalvar.isEnabled = true
                Toast.makeText(
                    this@ConfiguracoesActivity,
                    R.string.erro_enviar_foto,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                preferencias.salvarFotoPerfil(usuario.uid, caminho)
                concluirSalvamento()
            }
        }
    }

    // Confirma as alterações mantendo o comportamento original da tela.
    private fun concluirSalvamento() {
        val mensagem = if (emailAguardandoConfirmacao) {
            R.string.config_email_confirmacao
        } else {
            R.string.config_salvo
        }
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        finish()
    }
}
