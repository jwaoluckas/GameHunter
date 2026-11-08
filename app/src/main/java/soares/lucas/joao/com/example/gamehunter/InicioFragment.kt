package soares.lucas.joao.com.example.gamehunter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import soares.lucas.joao.com.example.gamehunter.adaptadores.ChipAdapter
import soares.lucas.joao.com.example.gamehunter.adaptadores.FileiraAdapter
import soares.lucas.joao.com.example.gamehunter.dados.Repositorios
import soares.lucas.joao.com.example.gamehunter.databinding.FragmentInicioBinding
import soares.lucas.joao.com.example.gamehunter.modais.ModalCompararMoedas
import soares.lucas.joao.com.example.gamehunter.modais.ModalFiltros
import soares.lucas.joao.com.example.gamehunter.modais.ModalJogo
import soares.lucas.joao.com.example.gamehunter.modelo.Fileira
import soares.lucas.joao.com.example.gamehunter.modelo.Filtros
import soares.lucas.joao.com.example.gamehunter.modelo.Genero

// Conteúdo da tela Principal: busca, chips de gênero e fileiras de jogos.
// É exibido dentro da PrincipalActivity
class InicioFragment : Fragment() {

    // Binding do Fragment. Fica null depois do onDestroyView para não manter na
    // memória Views que já saíram da tela; "binding" é o atalho sem o "?"
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    // Dados exibidos e filtros em uso: as fileiras vindas do repositório (vazias até
    // carregar), os gêneros selecionados e os filtros escolhidos no
    // modal. Os gêneros e os filtros são aplicados juntos
    private var todasAsFileiras: List<Fileira> = emptyList()
    private var carregamento: Job? = null
    private lateinit var chipsGeneros: ChipAdapter
    private var filtros = Filtros()

    private lateinit var fileiraAdapter: FileiraAdapter

    // Ciclo de vida do Fragment: onCreateView cria a View a partir do layout,
    // onViewCreated configura a tela e onDestroyView libera o binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarBusca()
        configurarChipsDeGenero()
        configurarFileiras()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Busca (RF02): a tecla "Pesquisar" do teclado abre a tela de resultados com o
    // texto digitado. O botão de filtros (RF05) abre o modal de filtros e ordenação
    private fun configurarBusca() {
        binding.edtBusca.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_SEARCH) {
                buscar(binding.edtBusca.text.toString().trim())
                true
            } else {
                false
            }
        }

        binding.btnFiltros.setOnClickListener {
            ModalFiltros(requireContext(), filtros) { novosFiltros ->
                filtros = novosFiltros
                marcarGeneroSelecionado()
                carregarFileiras()
            }.exibir()
        }
    }

    // Abre o Resultado de pesquisas enviando o texto como extra. Texto vazio mostra
    // um erro no campo
    private fun buscar(texto: String) {
        if (texto.isEmpty()) {
            binding.edtBusca.error = getString(R.string.erro_busca_vazia)
            return
        }

        esconderTeclado(binding.edtBusca)
        val intent = Intent(requireContext(), ResultadoPesquisaActivity::class.java)
        intent.putExtra(ResultadoPesquisaActivity.EXTRA_BUSCA, texto)
        intent.putExtra(ResultadoPesquisaActivity.EXTRA_GENEROS, filtros.generos.map { it.name }.toTypedArray())
        startActivity(intent)
    }

    // Chips de gênero em um RecyclerView horizontal. A posição 0 é "Todos" e as
    // seguintes são os gêneros do enum, na mesma ordem (por isso o "- 1")
    private fun configurarChipsDeGenero() {
        val textos = listOf(getString(R.string.principal_todos)) + Genero.entries.map { getString(it.texto) }

        binding.rvPlataformas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        chipsGeneros = ChipAdapter(textos) { posicao ->
            filtros = filtros.copy(generos = if (posicao == 0) emptySet() else setOf(Genero.entries[posicao - 1]))
            carregarFileiras()
        }
        binding.rvPlataformas.adapter = chipsGeneros
        marcarGeneroSelecionado()
    }

    // Fileiras em um RecyclerView vertical. O adapter recebe o que fazer ao tocar no
    // card (modal do jogo, RF03), em "Comparar moedas" (RF12) e em "Ver tudo"
    // (abre o Resultado de pesquisas com todos os jogos da fileira).
    // As fileiras são pedidas ao repositório dentro de uma corrotina, ligada ao ciclo
    // de vida da View do Fragment (é cancelada se a tela for fechada antes de terminar).
    private fun configurarFileiras() {
        fileiraAdapter = FileiraAdapter(
            aoClicarNoJogo = { jogo -> ModalJogo(requireContext(), jogo).exibir() },
            aoClicarEmCompararMoedas = { jogo -> ModalCompararMoedas(requireContext(), jogo).exibir() },
            aoClicarEmVerTudo = { fileira -> abrirFileiraCompleta(fileira) }
        )
        binding.rvFileiras.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFileiras.adapter = fileiraAdapter

        carregarFileiras()
    }

    private fun marcarGeneroSelecionado() {
        chipsGeneros.selecionar(when (filtros.generos.size) {
            0 -> 0
            1 -> filtros.generos.single().ordinal + 1
            else -> -1
        })
    }

    private fun carregarFileiras() {
        carregamento?.cancel()
        val generos = filtros.generos
        carregamento = viewLifecycleOwner.lifecycleScope.launch {
            fileiraAdapter.atualizarFileiras(emptyList())
            binding.txtNenhumJogo.setText(R.string.itad_carregando)
            binding.txtNenhumJogo.visibility = View.VISIBLE
            try {
                todasAsFileiras = Repositorios.jogos(requireContext()).fileiras(generos)
                atualizarFileiras()
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                binding.txtNenhumJogo.setText(mensagemErroItad(erro))
                binding.txtNenhumJogo.visibility = View.VISIBLE
            }
        }
    }

    private fun abrirFileiraCompleta(fileira: Fileira) {
        val intent = Intent(requireContext(), ResultadoPesquisaActivity::class.java)
        intent.putExtra(ResultadoPesquisaActivity.EXTRA_FILEIRA, fileira.titulo)
        intent.putExtra(ResultadoPesquisaActivity.EXTRA_GENEROS, filtros.generos.map { it.name }.toTypedArray())
        startActivity(intent)
    }

    // Em cada fileira, mantém só os jogos dos gêneros selecionados e aplica os filtros do
    // modal; as fileiras que ficarem vazias somem. Se nada sobrar, mostra a mensagem
    // "Nenhum jogo encontrado com esses filtros"
    private fun atualizarFileiras() {
        val fileirasFiltradas = todasAsFileiras
            .map { fileira -> fileira.copy(jogos = filtros.aplicar(fileira.jogos)) }
            .filter { it.jogos.isNotEmpty() }

        fileiraAdapter.atualizarFileiras(fileirasFiltradas)
        binding.txtNenhumJogo.setText(R.string.itad_sem_resultados)
        binding.txtNenhumJogo.visibility =
            if (fileirasFiltradas.isEmpty()) View.VISIBLE else View.GONE
    }
}
