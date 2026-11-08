package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.databinding.ItemMoedaBinding
import soares.lucas.joao.com.example.gamehunter.modais.textoPrecoRegional
import soares.lucas.joao.com.example.gamehunter.modelo.EstadoPrecoRegional
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Mostra a média das ofertas na moeda efetivamente retornada, sem câmbio fictício.
class MoedaAdapter(
    private val moedaPadrao: Moeda,
    private var moedas: List<Moeda>,
    private val aoTentarNovamente: (Moeda) -> Unit
) : RecyclerView.Adapter<MoedaAdapter.MoedaViewHolder>() {
    private var precos: Map<Moeda, EstadoPrecoRegional> = emptyMap()
    class MoedaViewHolder(val binding: ItemMoedaBinding) : RecyclerView.ViewHolder(binding.root)

    fun atualizarMoedas(novasMoedas: List<Moeda>) {
        moedas = novasMoedas
        notifyDataSetChanged()
    }

    fun atualizarPrecos(novosPrecos: Map<Moeda, EstadoPrecoRegional>) {
        precos = novosPrecos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoedaViewHolder =
        MoedaViewHolder(ItemMoedaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = moedas.size

    override fun onBindViewHolder(holder: MoedaViewHolder, position: Int) {
        val moeda = moedas[position]
        val estado = precos[moeda]
        with(holder.binding) {
            val contexto = root.context
            txtCodigoMoeda.text = moeda.name
            txtRegiaoMoeda.text = moeda.regiao
            txtTipoPreco.setText(R.string.card_media_hoje)
            txtValorMoeda.text = textoPrecoRegional(contexto, moeda, estado)
            txtValorConvertido.setText(when {
                estado is EstadoPrecoRegional.Falha -> R.string.moedas_tentar_novamente
                estado is EstadoPrecoRegional.SemOfertas -> R.string.moedas_sem_oferta_moeda
                estado !is EstadoPrecoRegional.Disponivel -> R.string.moedas_consultando
                moeda == moedaPadrao -> R.string.moedas_moeda_local
                else -> R.string.moedas_sem_conversao
            })
            root.setOnClickListener { if (estado is EstadoPrecoRegional.Falha) aoTentarNovamente(moeda) }
            root.isClickable = estado is EstadoPrecoRegional.Falha
        }
    }
}
