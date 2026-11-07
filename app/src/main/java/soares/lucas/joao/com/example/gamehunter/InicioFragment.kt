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
import soares.lucas.joao.com.example.gamehunter.adaptadores.ChipAdapter
import soares.lucas.joao.com.example.gamehunter.adaptadores.FileiraAdapter
import soares.lucas.joao.com.example.gamehunter.dados.Repositorios
import soares.lucas.joao.com.example.gamehunter.databinding.FragmentInicioBinding
import soares.lucas.joao.com.example.gamehunter.modais.ModalCompararMoedas
import soares.lucas.joao.com.example.gamehunter.modais.ModalFiltros
import soares.lucas.joao.com.example.gamehunter.modais.ModalJogo
import soares.lucas.joao.com.example.gamehunter.modelo.Fileira
import soares.lucas.joao.com.example.gamehunter.modelo.Filtros
import soares.lucas.joao.com.example.gamehunter.modelo.Plataforma

// Conteúdo da tela Principal: busca, chips de plataforma e fileiras de jogos.
// É exibido dentro da PrincipalActivity
class InicioFragment : Fragment() {

    // Binding do Fragment. Fica null depois do onDestroyView para não manter na
    // memória Views que já saíram da tela; "binding" é o atalho sem o "?"
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    // Dados exibidos e filtros em uso: as fileiras vindas do repositório (vazias até
    // carregar), a plataforma do chip (null = "Todos") e os filtros escolhidos no
    // modal. A plataforma e os filtros são aplicados juntos
    private var todasAsFileiras: List<Fileira> = emptyList()
    private var plataformaDoChip: Plataforma? = null
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
        configurarChipsDePlataforma()
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
                atualizarFileiras()
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
        startActivity(intent)
    }

    // Chips de plataforma em um RecyclerView horizontal. A posição 0 é "Todos" e as
    // seguintes são as plataformas do enum, na mesma ordem (por isso o "- 1")
    private fun configurarChipsDePlataforma() {
        val textos = listOf(getString(R.string.principal_todos)) + Plataforma.entries.map { it.nome }

        binding.rvPlataformas.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPlataformas.adapter = ChipAdapter(textos) { posicao ->
            plataformaDoChip = if (posicao == 0) null else Plataforma.entries[posicao - 1]
            atualizarFileiras()
        }
    }

    // Fileiras em um RecyclerView vertical. O adapter recebe o que fazer ao tocar no
    // card (modal do jogo, RF03), em "Comparar moedas" (RF12) e em "Ver tudo"
    // (abre o Resultado de pesquisas com todos os jogos da fileira).
    // As fileiras são pedidas ao repositório dentro de uma corrotina, ligada ao ciclo
    // de vida da View do Fragment (é cancelada se a tela for fechada antes de terminar).
    // TODO (Frontend): mostrar loading, erro e "sem internet" (RF10) quando o
    //  repositório real estiver pronto
    private fun configurarFileiras() {
        fileiraAdapter = FileiraAdapter(
            aoClicarNoJogo = { jogo -> ModalJogo(requireContext(), jogo).exibir() },
            aoClicarEmCompararMoedas = { jogo -> ModalCompararMoedas(requireContext(), jogo).exibir() },
            aoClicarEmVerTudo = { fileira -> abrirFileiraCompleta(fileira) }
        )
        binding.rvFileiras.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFileiras.adapter = fileiraAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            todasAsFileiras = Repositorios.jogos.fileiras()
            atualizarFileiras()
        }
    }

    private fun abrirFileiraCompleta(fileira: Fileira) {
        val intent = Intent(requireContext(), ResultadoPesquisaActivity::class.java)
        intent.putExtra(ResultadoPesquisaActivity.EXTRA_FILEIRA, fileira.titulo)
        startActivity(intent)
    }

    // Em cada fileira, mantém só os jogos da plataforma do chip e aplica os filtros do
    // modal; as fileiras que ficarem vazias somem. Se nada sobrar, mostra a mensagem
    // "Nenhum jogo encontrado com esses filtros"
    private fun atualizarFileiras() {
        val plataforma = plataformaDoChip

        val fileirasFiltradas = todasAsFileiras
            .map { fileira ->
                val jogosDaPlataforma = if (plataforma == null) {
                    fileira.jogos
                } else {
                    fileira.jogos.filter { plataforma in it.plataformas }
                }
                fileira.copy(jogos = filtros.aplicar(jogosDaPlataforma))
            }
            .filter { it.jogos.isNotEmpty() }

        fileiraAdapter.atualizarFileiras(fileirasFiltradas)
        binding.txtNenhumJogo.visibility =
            if (fileirasFiltradas.isEmpty()) View.VISIBLE else View.GONE
    }
}
