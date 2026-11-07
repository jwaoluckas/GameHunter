package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import soares.lucas.joao.com.example.gamehunter.databinding.ItemFileiraBinding
import soares.lucas.joao.com.example.gamehunter.modelo.Fileira
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo

// Adapter das FILEIRAS da tela Principal (lista vertical).
// Cada fileira tem outro RecyclerView, horizontal, que usa o JogoAdapter para os cards
class FileiraAdapter(
    private val aoClicarNoJogo: (Jogo) -> Unit,
    private val aoClicarEmCompararMoedas: (Jogo) -> Unit,
    private val aoClicarEmVerTudo: (Fileira) -> Unit
) : RecyclerView.Adapter<FileiraAdapter.FileiraViewHolder>() {

    // Lista exibida; muda quando o usuário filtra os jogos
    private var fileiras: List<Fileira> = emptyList()

    class FileiraViewHolder(val binding: ItemFileiraBinding) : RecyclerView.ViewHolder(binding.root)

    // Troca a lista exibida e avisa o RecyclerView para redesenhar
    fun atualizarFileiras(novasFileiras: List<Fileira>) {
        fileiras = novasFileiras
        notifyDataSetChanged()
    }

    // Ao criar uma fileira, já define que o carrossel de cards rola na horizontal
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileiraViewHolder {
        val binding = ItemFileiraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.rvJogos.layoutManager =
            LinearLayoutManager(parent.context, LinearLayoutManager.HORIZONTAL, false)
        return FileiraViewHolder(binding)
    }

    // Preenche título e subtítulo, cria o adapter dos cards desta fileira e
    // configura o clique em "Ver tudo"
    override fun onBindViewHolder(holder: FileiraViewHolder, position: Int) {
        val fileira = fileiras[position]

        holder.binding.txtTituloFileira.text = fileira.titulo
        holder.binding.txtSubtituloFileira.text = fileira.subtitulo
        holder.binding.rvJogos.adapter =
            JogoAdapter(fileira.jogos, aoClicarNoJogo, aoClicarEmCompararMoedas)
        holder.binding.txtVerTudo.setOnClickListener { aoClicarEmVerTudo(fileira) }
    }

    override fun getItemCount(): Int = fileiras.size
}
