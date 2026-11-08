package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import soares.lucas.joao.com.example.gamehunter.databinding.ItemPrecoMoedaBinding
import soares.lucas.joao.com.example.gamehunter.modais.textoPrecoRegional
import soares.lucas.joao.com.example.gamehunter.modelo.EstadoPrecoRegional
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Mesmas caixas do modal de detalhes, preenchidas por consultas regionais reais.
class PrecoMoedaAdapter(
    private var moedas: List<Moeda>,
    private val aoTentarNovamente: (Moeda) -> Unit
) : RecyclerView.Adapter<PrecoMoedaAdapter.PrecoMoedaViewHolder>() {
    private var precos: Map<Moeda, EstadoPrecoRegional> = emptyMap()
    class PrecoMoedaViewHolder(val binding: ItemPrecoMoedaBinding) : RecyclerView.ViewHolder(binding.root)

    fun atualizarMoedas(novasMoedas: List<Moeda>) {
        moedas = novasMoedas
        notifyDataSetChanged()
    }

    fun atualizarPrecos(novosPrecos: Map<Moeda, EstadoPrecoRegional>) {
        precos = novosPrecos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrecoMoedaViewHolder =
        PrecoMoedaViewHolder(ItemPrecoMoedaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = moedas.size

    override fun onBindViewHolder(holder: PrecoMoedaViewHolder, position: Int) {
        val moeda = moedas[position]
        holder.binding.txtRegiao.text = moeda.regiaoCurta
        holder.binding.txtValor.text = textoPrecoRegional(holder.binding.root.context, moeda, precos[moeda])
        val falhou = precos[moeda] is EstadoPrecoRegional.Falha
        holder.binding.root.setOnClickListener { if (falhou) aoTentarNovamente(moeda) }
        holder.binding.root.isClickable = falhou
    }
}
