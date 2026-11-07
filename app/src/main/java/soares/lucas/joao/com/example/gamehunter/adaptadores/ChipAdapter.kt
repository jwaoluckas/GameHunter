package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import soares.lucas.joao.com.example.gamehunter.databinding.ItemChipBinding

// Adapter de uma lista horizontal de chips em que só UM fica selecionado por vez
// (chips de plataforma da tela Principal e chips rápidos do Resultado de pesquisas).
// Recebe os textos dos chips e avisa a tela qual posição foi escolhida
class ChipAdapter(
    private val opcoes: List<String>,
    private val aoSelecionar: (Int) -> Unit
) : RecyclerView.Adapter<ChipAdapter.ChipViewHolder>() {

    // Começa com o primeiro chip ("Todos"/"Todas") selecionado
    private var posicaoSelecionada = 0

    class ChipViewHolder(val binding: ItemChipBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChipViewHolder(binding)
    }

    override fun getItemCount(): Int = opcoes.size

    // Preenche o chip e marca se ele é o selecionado (o fundo e a cor do texto mudam
    // sozinhos pelo isSelected). Ao tocar, marca o novo chip, redesenha só os dois
    // chips que mudaram e avisa a tela qual posição foi escolhida
    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val chip = holder.binding.txtChip
        chip.text = opcoes[position]
        chip.isSelected = position == posicaoSelecionada

        chip.setOnClickListener {
            val posicaoAnterior = posicaoSelecionada
            posicaoSelecionada = holder.bindingAdapterPosition
            notifyItemChanged(posicaoAnterior)
            notifyItemChanged(posicaoSelecionada)
            aoSelecionar(posicaoSelecionada)
        }
    }
}
