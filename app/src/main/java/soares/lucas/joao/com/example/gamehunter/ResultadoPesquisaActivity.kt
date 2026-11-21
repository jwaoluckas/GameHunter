package soares.lucas.joao.com.example.gamehunter

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import soares.lucas.joao.com.example.gamehunter.modelo.Loja
import soares.lucas.joao.com.example.gamehunter.modelo.Ordenacao
import soares.lucas.joao.com.example.gamehunter.modelo.Genero
import soares.lucas.joao.com.example.gamehunter.modelo.SelecaoConteudo

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
        const val EXTRA_ORDENACAO = "extra_ordenacao"
        const val EXTRA_CONTEUDO = "extra_conteudo"
        const val EXTRA_LOJAS = "extra_lojas"

        private const val CHIP_TODAS = 0
        private const val CHIP_EM_PROMOCAO = 1
        private const val PRIMEIRO_GENERO = 2
        private val CHIP_MENOR_PRECO = PRIMEIRO_GENERO + Genero.entries.size
        private const val COLUNAS = 2
        private const val TAMANHO_PAGINA = 20
        private const val ITENS_ANTES_DO_FIM = 4
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
    private var proximoOffset = 0
    private var temMaisPaginas = false
    private var carregandoPagina = false
    private var versaoConsulta = 0

    // Cria a tela, carrega os jogos de acordo com o extra recebido e configura
    // a busca, os chips e a grade
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultadoPesquisaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filtros = filtros.copy(
            selecaoConteudo = intent.getStringExtra(EXTRA_CONTEUDO)
                ?.let { nome -> SelecaoConteudo.entries.find { it.name == nome } }
                ?: SelecaoConteudo.JOGOS,
            generos = intent.getStringArrayExtra(EXTRA_GENEROS).orEmpty()
                .mapNotNull { nome -> Genero.entries.find { it.name == nome } }.toSet(),
            lojas = intent.getStringArrayExtra(EXTRA_LOJAS).orEmpty()
                .mapNotNull { nome -> Loja.entries.find { it.name == nome } }.toSet(),
            ordenacao = intent.getStringExtra(EXTRA_ORDENACAO)
                ?.let { nome -> Ordenacao.entries.find { it.name == nome } }
                ?: Ordenacao.RELEVANCIA
        )
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
        if (titulo == null) atualizarResultados()
        else reiniciarPaginacao()
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
        binding.rvResultados.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val grade = recyclerView.layoutManager as GridLayoutManager
                val ultimoVisivel = grade.findLastVisibleItemPosition()
                if (ultimoVisivel >= jogoAdapter.itemCount - ITENS_ANTES_DO_FIM) {
                    carregarProximaPagina()
                }
            }
        })
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
        versaoConsulta++
        temMaisPaginas = false
        carregandoPagina = false
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

    private fun reiniciarPaginacao() {
        buscaEmAndamento?.cancel()
        versaoConsulta++
        proximoOffset = 0
        temMaisPaginas = true
        carregandoPagina = false
        jogosEncontrados = emptyList()
        atualizarResultados()
        binding.txtNenhumResultado.setText(R.string.itad_carregando)
        binding.txtNenhumResultado.visibility = View.VISIBLE
        carregarProximaPagina()
    }

    private fun carregarProximaPagina() {
        val titulo = tituloDaFileira ?: return
        if (carregandoPagina || !temMaisPaginas) return
        val filtrosEmUso = filtrosComChipRapido()
        val offsetDaPagina = proximoOffset
        val versao = versaoConsulta
        carregandoPagina = true
        buscaEmAndamento = lifecycleScope.launch {
            var carregou = false
            try {
                val pagina = Repositorios.jogos(this@ResultadoPesquisaActivity).paginaDaFileira(
                    titulo,
                    filtrosEmUso.generos,
                    filtrosEmUso.ordenacao,
                    filtrosEmUso.selecaoConteudo,
                    filtrosEmUso.lojas,
                    offsetDaPagina,
                    TAMANHO_PAGINA
                )
                if (versao != versaoConsulta) return@launch
                jogosEncontrados = (jogosEncontrados + pagina.jogos)
                    .distinctBy { it.idItad ?: it.id.toString() }
                proximoOffset += TAMANHO_PAGINA
                temMaisPaginas = pagina.temMais
                carregou = true
                atualizarResultados()
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                if (versao == versaoConsulta) {
                    binding.txtNenhumResultado.setText(mensagemErroItad(erro))
                    binding.txtNenhumResultado.visibility = View.VISIBLE
                }
            } finally {
                if (versao == versaoConsulta) {
                    carregandoPagina = false
                    if (carregou) carregarMaisSeAListaNaoPreencheATela()
                }
            }
        }
    }

    private fun carregarMaisSeAListaNaoPreencheATela() {
        binding.rvResultados.post {
            if (temMaisPaginas && !carregandoPagina &&
                (jogoAdapter.itemCount == 0 || !binding.rvResultados.canScrollVertically(1))) {
                carregarProximaPagina()
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
