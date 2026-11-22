package soares.lucas.joao.com.example.gamehunter

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import soares.lucas.joao.com.example.gamehunter.adaptadores.JogoAdapter
import soares.lucas.joao.com.example.gamehunter.dados.RepositorioFavoritos
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityFavoritosBinding
import soares.lucas.joao.com.example.gamehunter.modais.ModalCompararMoedas
import soares.lucas.joao.com.example.gamehunter.modais.ModalJogo
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo

// Tela de FAVORITOS, aberta pelo menu de perfil. Lista, em uma grade de 2 colunas com
// o mesmo card das outras telas, os jogos que o usuário logado favoritou na estrela
// do modal do jogo. Os dados ficam somente no armazenamento local do dispositivo.
class FavoritosActivity : AppCompatActivity() {

    companion object {
        private const val COLUNAS = 2
    }

    private lateinit var binding: ActivityFavoritosBinding
    private lateinit var jogoAdapter: JogoAdapter
    private val favoritos by lazy { RepositorioFavoritos(this) }
    private var registroFavoritos: AutoCloseable? = null

    // Cria a tela, configura o voltar e a grade e começa mostrando o carregamento
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVoltar.setOnClickListener { finish() }
        configurarGrade()
        mostrarCarregando()
    }

    // A lista local é observada enquanto a tela está visível: o repositório chama
    // mostrarFavoritos de novo a cada mudança. Assim, se o usuário desfavoritar um jogo
    // pelo modal, o card some da grade sem precisar recarregar a tela.
    // O "ouvinte" é removido sozinho no onStop, por isso é criado aqui no onStart.
    // Sem usuário logado (sessão expirada), avisa e fecha a tela
    override fun onStart() {
        super.onStart()
        registroFavoritos?.close()
        registroFavoritos = favoritos.observar(
            aoReceber = { jogos -> mostrarFavoritos(jogos) },
            aoFalhar = {
                binding.progressoFavoritos.visibility = View.GONE
                Toast.makeText(this, R.string.erro_favoritos_carregar, Toast.LENGTH_SHORT).show()
            }
        )

        if (registroFavoritos == null) {
            Toast.makeText(this, R.string.erro_favoritos_carregar, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStop() {
        registroFavoritos?.close()
        registroFavoritos = null
        super.onStop()
    }

    // Grade de 2 colunas reaproveitando o JogoAdapter (emGrade = true), como na tela de
    // Resultado de pesquisas. Tocar no card abre o mesmo modal do jogo, onde fica a estrela
    private fun configurarGrade() {
        jogoAdapter = JogoAdapter(
            jogos = emptyList(),
            aoClicarNoJogo = { jogo -> ModalJogo(this, jogo).exibir() },
            aoClicarEmCompararMoedas = { jogo -> ModalCompararMoedas(this, jogo).exibir() },
            emGrade = true
        )
        binding.rvFavoritos.layoutManager = GridLayoutManager(this, COLUNAS)
        binding.rvFavoritos.adapter = jogoAdapter
    }

    private fun mostrarCarregando() {
        binding.progressoFavoritos.visibility = View.VISIBLE
        binding.rvFavoritos.visibility = View.GONE
        binding.estadoVazio.visibility = View.GONE
        binding.txtSubtitulo.visibility = View.GONE
    }

    // Esconde o carregamento e mostra a grade (com o texto de apoio) ou, se a lista
    // estiver vazia, a mensagem com a estrela vazia
    private fun mostrarFavoritos(jogos: List<Jogo>) {
        jogoAdapter.atualizarJogos(jogos)

        val vazio = jogos.isEmpty()
        binding.progressoFavoritos.visibility = View.GONE
        binding.rvFavoritos.visibility = if (vazio) View.GONE else View.VISIBLE
        binding.txtSubtitulo.visibility = if (vazio) View.GONE else View.VISIBLE
        binding.estadoVazio.visibility = if (vazio) View.VISIBLE else View.GONE
    }
}
