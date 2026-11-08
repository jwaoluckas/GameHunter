package soares.lucas.joao.com.example.gamehunter

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import soares.lucas.joao.com.example.gamehunter.adaptadores.ChipAdapter
import soares.lucas.joao.com.example.gamehunter.adaptadores.JogoAdapter
import soares.lucas.joao.com.example.gamehunter.dados.Repositorios
import soares.lucas.joao.com.example.gamehunter.databinding.ActivityResultadoPesquisaBinding
import soares.lucas.joao.com.example.gamehunter.modais.ModalCompararMoedas
import soares.lucas.joao.com.example.gamehunter.modais.ModalFiltros
import soares.lucas.joao.com.example.gamehunter.modais.ModalJogo
import soares.lucas.joao.com.example.gamehunter.modelo.Filtros
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Ordenacao
import soares.lucas.joao.com.example.gamehunter.modelo.Genero

// Tela de RESULTADO DE PESQUISAS (RF02). Pode ser aberta de dois jeitos:
// - pela busca da tela Principal: recebe o texto digitado em EXTRA_BUSCA
// - pelo "Ver tudo" de uma fileira: recebe o título da fileira em EXTRA_FILEIRA
// Os jogos aparecem em uma grade de 2 colunas e podem ser filtrados pelos chips
// rápidos e pelo modal de filtros
class ResultadoPesquisaActivity : AppCompatActivity() {

    // Chaves dos extras e posições dos chips rápidos
    // (Todos, Em promoção, gêneros e Menor preço)
    companion object {
        const val EXTRA_BUSCA = "extra_busca"
        const val EXTRA_FILEIRA = "extra_fileira"
        const val EXTRA_GENEROS = "extra_generos"

        private const val CHIP_TODAS = 0
        private const val CHIP_EM_PROMOCAO = 1
        private const val PRIMEIRO_GENERO = 2
        private val CHIP_MENOR_PRECO = PRIMEIRO_GENERO + Genero.entries.size
        private const val COLUNAS = 2
    }

    private lateinit var binding: ActivityResultadoPesquisaBinding
    private lateinit var jogoAdapter: JogoAdapter

    // Estado da tela: jogos encontrados (antes dos filtros), texto exibido na
    // contagem ("para “God of War”" ou "em “Mais vendidos”"), filtros do modal
    // e chip rápido selecionado
    private var jogosEncontrados: List<Jogo> = emptyList()
    private var descricaoDaPesquisa = ""
    private var filtros = Filtros()
    private var chipRapido = CHIP_TODAS
    private var buscaEmAndamento: Job? = null
    private lateinit var chipsGeneros: ChipAdapter
    private var tituloDaFileira: String? = null

    // Cria a tela, carrega os jogos de acordo com o extra recebido e configura
    // a busca, os chips e a grade
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultadoPesquisaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filtros = filtros.copy(generos = intent.getStringArrayExtra(EXTRA_GENEROS).orEmpty()
            .mapNotNull { nome -> Genero.entries.find { it.name == nome } }.toSet())
        configurarBusca()
        configurarChipsRapidos()
        configurarGrade()
        carregarJogosDoExtra()
    }

    // Se veio de "Ver tudo", pede ao repositório os jogos da fileira (dentro de uma
    // corrotina); senão, faz a busca pelo texto
    private fun carregarJogosDoExtra() {
        val tituloFileira = intent.getStringExtra(EXTRA_FILEIRA)
        tituloDaFileira = tituloFileira

        if (tituloFileira != null) {
            descricaoDaPesquisa = getString(R.string.resultado_em, tituloFileira)
            recarregarFileiraOuFiltrar()
        } else {
            val texto = intent.getStringExtra(EXTRA_BUSCA).orEmpty()
            binding.edtBusca.setText(texto)
            buscar(texto)
        }
    }

    // Linha de busca: voltar fecha a tela; a tecla "Pesquisar" do teclado faz uma nova
    // busca; o X limpa o campo e deixa o cursor nele; o botão de filtros e o texto
    // "Ordenar" abrem o modal de filtros
    private fun configurarBusca() {
        binding.btnVoltar.setOnClickListener { finish() }

        binding.edtBusca.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_SEARCH) {
                val texto = binding.edtBusca.text.toString().trim()
                if (texto.isEmpty()) {
                    binding.edtBusca.error = getString(R.string.erro_busca_vazia)
                } else {
                    esconderTeclado(binding.edtBusca)
                    buscar(texto)
                }
                true
            } else {
                false
            }
        }

        binding.btnLimpar.setOnClickListener {
            binding.edtBusca.text.clear()
            binding.edtBusca.requestFocus()
        }

        binding.btnFiltros.setOnClickListener { abrirFiltros() }
        binding.txtOrdenar.setOnClickListener { abrirFiltros() }
    }

    // Chips rápidos em uma lista horizontal, com um único chip selecionado
    private fun configurarChipsRapidos() {
        val textos = listOf(getString(R.string.principal_todos), getString(R.string.resultado_em_promocao)) +
            Genero.entries.map { getString(it.texto) } + getString(R.string.ordenar_menor_preco)

        binding.rvChips.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        chipsGeneros = ChipAdapter(textos) { posicao ->
            chipRapido = posicao
            filtros = filtros.copy(generos = Genero.entries.getOrNull(posicao - PRIMEIRO_GENERO)
                ?.let { setOf(it) } ?: emptySet())
            recarregarFileiraOuFiltrar()
        }
        binding.rvChips.adapter = chipsGeneros
        marcarGeneroSelecionado()
    }

    private fun marcarGeneroSelecionado() {
        chipsGeneros.selecionar(when (filtros.generos.size) {
            0 -> chipRapido
            1 -> filtros.generos.single().ordinal + PRIMEIRO_GENERO
            else -> -1
        })
    }

    private fun recarregarFileiraOuFiltrar() {
        val titulo = tituloDaFileira
        val generos = filtros.generos
        if (titulo == null) atualizarResultados()
        else carregarResultados { Repositorios.jogos(this).jogosDaFileira(titulo, generos) }
    }

    // Grade de 2 colunas (GridLayoutManager) usando o mesmo card da tela Principal.
    // Tocar no card abre os detalhes; "Comparar moedas" abre o modal de moedas
    private fun configurarGrade() {
        jogoAdapter = JogoAdapter(
            jogos = emptyList(),
            aoClicarNoJogo = { jogo -> ModalJogo(this, jogo).exibir() },
            aoClicarEmCompararMoedas = { jogo -> ModalCompararMoedas(this, jogo).exibir() },
            emGrade = true
        )
        binding.rvResultados.layoutManager = GridLayoutManager(this, COLUNAS)
        binding.rvResultados.adapter = jogoAdapter
    }

    // Busca os jogos pelo título no repositório, dentro de uma corrotina ligada ao
    // ciclo de vida da tela (é cancelada se a tela for fechada antes de terminar).
    private fun buscar(texto: String) {
        tituloDaFileira = null
        descricaoDaPesquisa = getString(R.string.resultado_para, texto)
        carregarResultados { Repositorios.jogos(this).buscarPorTitulo(texto) }
    }

    // Cancela a busca anterior para não sobrescrever uma consulta mais recente.
    private fun carregarResultados(consulta: suspend () -> List<Jogo>) {
        buscaEmAndamento?.cancel()
        buscaEmAndamento = lifecycleScope.launch {
            jogosEncontrados = emptyList()
            atualizarResultados()
            binding.txtNenhumResultado.setText(R.string.itad_carregando)
            binding.txtNenhumResultado.visibility = View.VISIBLE
            try {
                jogosEncontrados = consulta()
                atualizarResultados()
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                binding.txtNenhumResultado.setText(mensagemErroItad(erro))
                binding.txtNenhumResultado.visibility = View.VISIBLE
            }
        }
    }

    // Abre o modal de filtros com os filtros atuais e, ao aplicar, refaz a lista
    private fun abrirFiltros() {
        ModalFiltros(this, filtros) { novosFiltros ->
            filtros = novosFiltros
            chipRapido = CHIP_TODAS
            marcarGeneroSelecionado()
            recarregarFileiraOuFiltrar()
        }.exibir()
    }

    // O chip rápido escolhido é somado aos filtros do modal:
    // - "Em promoção" liga o filtro de promoção
    // - Gêneros são aplicados pelas tags completas de cada jogo
    // - "Menor preço" ordena do mais barato para o mais caro
    private fun filtrosComChipRapido(): Filtros {
        return when (chipRapido) {
            CHIP_EM_PROMOCAO -> filtros.copy(somentePromocao = true)
            CHIP_MENOR_PRECO -> filtros.copy(ordenacao = Ordenacao.MENOR_PRECO)
            else -> filtros
        }
    }

    // Aplica os filtros, atualiza a grade, a contagem ("6 resultados para “God of War”"),
    // o texto "Ordenar: ..." e a mensagem de nenhum resultado
    private fun atualizarResultados() {
        val filtrosEmUso = filtrosComChipRapido()
        val resultados = filtrosEmUso.aplicar(jogosEncontrados)

        jogoAdapter.atualizarJogos(resultados)

        binding.txtContagem.text = if (resultados.size == 1) {
            getString(R.string.resultado_contagem_um, descricaoDaPesquisa)
        } else {
            getString(R.string.resultado_contagem, resultados.size, descricaoDaPesquisa)
        }
        binding.txtOrdenar.text = getString(R.string.resultado_ordenar, getString(filtrosEmUso.ordenacao.texto))

        binding.txtNenhumResultado.setText(R.string.itad_sem_resultados)
        binding.txtNenhumResultado.visibility = if (resultados.isEmpty()) View.VISIBLE else View.GONE
    }
}
