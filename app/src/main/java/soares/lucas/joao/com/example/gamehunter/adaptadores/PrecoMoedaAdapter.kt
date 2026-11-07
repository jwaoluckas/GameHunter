package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import soares.lucas.joao.com.example.gamehunter.databinding.ItemPrecoMoedaBinding
import soares.lucas.joao.com.example.gamehunter.formatarValorNaMoeda
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Adapter das caixas "Comparar moedas" do modal do jogo (ex.: "EUA / US$ 69,99"),
// uma para cada moeda escolhida pelo usuário
class PrecoMoedaAdapter(
    private val jogo: Jogo,
    private var moedas: List<Moeda>
) : RecyclerView.Adapter<PrecoMoedaAdapter.PrecoMoedaViewHolder>() {

    class PrecoMoedaViewHolder(val binding: ItemPrecoMoedaBinding) : RecyclerView.ViewHolder(binding.root)

    // Troca as moedas exibidas (depois de "Aplicar moedas") e redesenha
    fun atualizarMoedas(novasMoedas: List<Moeda>) {
        moedas = novasMoedas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrecoMoedaViewHolder {
        val binding = ItemPrecoMoedaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PrecoMoedaViewHolder(binding)
    }

    override fun getItemCount(): Int = moedas.size

    // Preenche a caixa com a região e o preço do jogo nessa moeda
    override fun onBindViewHolder(holder: PrecoMoedaViewHolder, position: Int) {
        val moeda = moedas[position]

        holder.binding.txtRegiao.text = moeda.regiaoCurta
        holder.binding.txtValor.text =
            formatarValorNaMoeda(holder.binding.root.context, moeda, moeda.precoDoJogo(jogo))
    }
}
