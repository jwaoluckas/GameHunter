package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.databinding.ItemMoedaBinding
import soares.lucas.joao.com.example.gamehunter.formatarValorNaMoeda
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Adapter da lista de moedas do modal "Comparar moedas": mostra o preço do jogo em
// cada moeda escolhida e o mesmo valor convertido para a moeda padrão do usuário
class MoedaAdapter(
    private val jogo: Jogo,
    private val moedaPadrao: Moeda,
    private var moedas: List<Moeda>
) : RecyclerView.Adapter<MoedaAdapter.MoedaViewHolder>() {

    class MoedaViewHolder(val binding: ItemMoedaBinding) : RecyclerView.ViewHolder(binding.root)

    // Troca as moedas exibidas (quando um chip é marcado ou desmarcado) e redesenha
    fun atualizarMoedas(novasMoedas: List<Moeda>) {
        moedas = novasMoedas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoedaViewHolder {
        val binding = ItemMoedaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MoedaViewHolder(binding)
    }

    override fun getItemCount(): Int = moedas.size

    // Preenche a linha da moeda:
    // - no Brasil o valor é a média das lojas ("Média hoje"); nas outras regiões é o
    //   preço de tabela
    // - a moeda padrão mostra "Moeda local"; as demais mostram "≈" e o valor convertido
    override fun onBindViewHolder(holder: MoedaViewHolder, position: Int) {
        val moeda = moedas[position]
        val valor = moeda.precoDoJogo(jogo)

        with(holder.binding) {
            val contexto = root.context

            txtCodigoMoeda.text = moeda.name
            txtRegiaoMoeda.text = moeda.regiao
            txtTipoPreco.setText(if (moeda == Moeda.BRL) R.string.card_media_hoje else R.string.moedas_preco_tabela)
            txtValorMoeda.text = formatarValorNaMoeda(contexto, moeda, valor)

            txtValorConvertido.text = if (moeda == moedaPadrao) {
                contexto.getString(R.string.moedas_moeda_local)
            } else {
                val convertido = moeda.converter(valor, moedaPadrao)
                contexto.getString(R.string.moedas_aproximado, formatarValorNaMoeda(contexto, moedaPadrao, convertido))
            }
        }
    }
}
