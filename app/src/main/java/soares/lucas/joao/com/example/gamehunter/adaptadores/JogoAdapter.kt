package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.databinding.ItemJogoBinding
import soares.lucas.joao.com.example.gamehunter.formatarNota
import soares.lucas.joao.com.example.gamehunter.formatarPreco
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.preencherEtiquetasDePlataforma

// Adapter dos CARDS DE JOGO. É usado no carrossel de cada fileira da tela Principal
// e na grade da tela de Resultado de pesquisas (emGrade = true).
// As funções de clique são recebidas de fora, para que a tela decida o que fazer
class JogoAdapter(
    private var jogos: List<Jogo>,
    private val aoClicarNoJogo: (Jogo) -> Unit,
    private val aoClicarEmCompararMoedas: (Jogo) -> Unit,
    private val emGrade: Boolean = false
) : RecyclerView.Adapter<JogoAdapter.JogoViewHolder>() {

    class JogoViewHolder(val binding: ItemJogoBinding) : RecyclerView.ViewHolder(binding.root)

    // Troca a lista exibida e avisa o RecyclerView para redesenhar
    fun atualizarJogos(novosJogos: List<Jogo>) {
        jogos = novosJogos
        notifyDataSetChanged()
    }

    // No carrossel o card tem largura fixa (156dp, definida no layout). Na grade ele
    // ocupa toda a largura da coluna e ganha um espaço de 20dp abaixo, separando as linhas
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JogoViewHolder {
        val binding = ItemJogoBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        if (emGrade) {
            val parametros = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            parametros.width = ViewGroup.LayoutParams.MATCH_PARENT
            parametros.bottomMargin = (20 * parent.resources.displayMetrics.density).toInt()
            binding.root.layoutParams = parametros
        }

        return JogoViewHolder(binding)
    }

    override fun getItemCount(): Int = jogos.size

    // Preenche o card com os dados do jogo: capa, nota, título, preços, plataformas,
    // descrições para o leitor de tela (RF14) e os cliques.
    // Enquanto não há imagem, a capa mostra o título sobre um degradê.
    // TODO (API): carregar a imagem de jogo.capaUrl em imgCapa (ex.: com Glide ou Coil)
    override fun onBindViewHolder(holder: JogoViewHolder, position: Int) {
        val jogo = jogos[position]

        with(holder.binding) {
            val contexto = root.context

            txtTituloCapa.text = jogo.titulo
            frameCapa.clipToOutline = true

            txtNota.text = formatarNota(jogo.nota)
            txtTitulo.text = jogo.titulo
            txtPrecoLancamento.text = contexto.getString(R.string.formato_preco, formatarPreco(jogo.precoLancamento))
            txtPrecoMedio.text = contexto.getString(R.string.formato_preco, formatarPreco(jogo.precoMedioHoje))
            preencherEtiquetasDePlataforma(listOf(txtPlataforma1, txtPlataforma2, txtPlataforma3), jogo.plataformas)

            cardJogo.contentDescription = contexto.getString(
                R.string.desc_card_jogo, jogo.titulo, txtNota.text, txtPrecoMedio.text
            )
            imgCapa.contentDescription = contexto.getString(R.string.desc_capa_jogo, jogo.titulo)

            cardJogo.setOnClickListener { aoClicarNoJogo(jogo) }
            btnCompararMoedas.setOnClickListener { aoClicarEmCompararMoedas(jogo) }
        }
    }
}
