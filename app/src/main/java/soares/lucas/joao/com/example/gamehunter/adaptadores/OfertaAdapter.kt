package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.databinding.ItemOfertaBinding
import soares.lucas.joao.com.example.gamehunter.formatarPreco
import soares.lucas.joao.com.example.gamehunter.modelo.Oferta

// Adapter da lista "Onde comprar" do modal do jogo. As ofertas chegam ordenadas da
// mais barata para a mais cara; a primeira recebe o texto "Menor preço".
// O que fazer ao tocar em "Ir" é decidido pelo modal (aoClicarEmIr)
class OfertaAdapter(
    private var ofertas: List<Oferta>,
    private val aoClicarEmIr: (Oferta) -> Unit
) : RecyclerView.Adapter<OfertaAdapter.OfertaViewHolder>() {

    class OfertaViewHolder(val binding: ItemOfertaBinding) : RecyclerView.ViewHolder(binding.root)

    // Troca a lista exibida (ex.: ao tocar em "Ver mais lojas") e redesenha
    fun atualizarOfertas(novasOfertas: List<Oferta>) {
        ofertas = novasOfertas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfertaViewHolder {
        val binding = ItemOfertaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfertaViewHolder(binding)
    }

    override fun getItemCount(): Int = ofertas.size

    // Preenche a linha: inicial e nome da loja, plataforma, preço e o botão "Ir"
    // (com uma descrição para o leitor de tela dizendo qual loja será aberta)
    override fun onBindViewHolder(holder: OfertaViewHolder, position: Int) {
        val oferta = ofertas[position]

        with(holder.binding) {
            val contexto = root.context

            txtInicialLoja.text = oferta.nomeLoja.take(1)
            txtNomeLoja.text = oferta.nomeLoja
            txtDetalheLoja.text = if (position == 0) {
                contexto.getString(R.string.oferta_menor_preco, oferta.plataforma)
            } else {
                oferta.plataforma
            }
            txtPrecoOferta.text = contexto.getString(R.string.formato_preco, formatarPreco(oferta.preco))

            btnIr.contentDescription = contexto.getString(R.string.desc_ir_loja, oferta.nomeLoja)
            btnIr.setOnClickListener { aoClicarEmIr(oferta) }
        }
    }
}
