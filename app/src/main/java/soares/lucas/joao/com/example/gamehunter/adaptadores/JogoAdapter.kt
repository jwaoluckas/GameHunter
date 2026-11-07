package soares.lucas.joao.com.example.gamehunter.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.PathInterpolator
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

    // Valores da animação de toque, iguais aos do protótipo no Figma (o card troca para a
    // variante "Pressionado" com Smart Animate e, 200ms depois, abre os detalhes):
    // - ESCALA_PRESSIONADO: o card cresce ~6% (de 156 para 166 de largura)
    // - DURACAO_ANIMACAO: 150ms, com a curva "ease-out" (começa rápido e desacelera)
    // - ESPERA_ANTES_DE_ABRIR: tempo parado no estado pressionado antes de abrir o modal
    // - ELEVACAO_PRESSIONADO: põe o card na frente dos vizinhos, para o brilho não
    //   ficar escondido atrás deles
    companion object {
        private const val ESCALA_PRESSIONADO = 1.064f
        private const val DURACAO_ANIMACAO = 150L
        private const val ESPERA_ANTES_DE_ABRIR = 200L
        private const val ELEVACAO_PRESSIONADO = 1f
        private val CURVA_EASE_OUT = PathInterpolator(0f, 0f, 0.58f, 1f)
    }

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
    // descrições para o leitor de tela (RF14) e os cliques. Como o RecyclerView
    // reaproveita os cards, o estado da animação de toque é zerado antes de preencher.
    // Tocar no card faz a animação e só depois abre os detalhes.
    // Enquanto não há imagem, a capa mostra o título sobre um degradê.
    // TODO (API): carregar a imagem de jogo.capaUrl em imgCapa (ex.: com Glide ou Coil)
    override fun onBindViewHolder(holder: JogoViewHolder, position: Int) {
        val jogo = jogos[position]

        with(holder.binding) {
            val contexto = root.context
            voltarAoEstadoPadrao(this, animar = false)

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

            cardJogo.setOnClickListener {
                animarToque(this) { aoClicarNoJogo(jogo) }
            }
            btnCompararMoedas.setOnClickListener { aoClicarEmCompararMoedas(jogo) }
        }
    }

    // Animação de toque: o card cresce a partir do centro da capa, e a borda e o brilho
    // laranja aparecem (alpha de 0 para 1), tudo ao mesmo tempo. Ao terminar, espera um
    // pouco no estado pressionado, executa "aoTerminar" (abre os detalhes) e volta ao
    // normal. O card fica desabilitado durante a animação para ignorar toques repetidos.
    // Se o usuário desligar as animações do Android, elas acontecem instantaneamente
    private fun animarToque(card: ItemJogoBinding, aoTerminar: () -> Unit) {
        val raiz = card.cardJogo
        raiz.isEnabled = false
        raiz.pivotX = raiz.width / 2f
        raiz.pivotY = card.frameCapa.height / 2f

        raiz.animate()
            .scaleX(ESCALA_PRESSIONADO)
            .scaleY(ESCALA_PRESSIONADO)
            .translationZ(ELEVACAO_PRESSIONADO)
            .setDuration(DURACAO_ANIMACAO)
            .setInterpolator(CURVA_EASE_OUT)
            .withEndAction {
                raiz.postDelayed({
                    aoTerminar()
                    voltarAoEstadoPadrao(card, animar = true)
                }, ESPERA_ANTES_DE_ABRIR)
            }
            .start()

        listOf(card.viewBrilho, card.viewBordaDestaque).forEach { destaque ->
            destaque.animate()
                .alpha(1f)
                .setDuration(DURACAO_ANIMACAO)
                .setInterpolator(CURVA_EASE_OUT)
                .start()
        }
    }

    // Desfaz a animação de toque: tamanho normal, sem borda e sem brilho, card habilitado.
    // Com animar = false (card sendo reaproveitado) tudo volta na hora; com true, volta
    // com a mesma duração e curva da ida, enquanto o modal do jogo abre por cima
    private fun voltarAoEstadoPadrao(card: ItemJogoBinding, animar: Boolean) {
        val raiz = card.cardJogo
        val destaques = listOf(card.viewBrilho, card.viewBordaDestaque)
        val duracao = if (animar) DURACAO_ANIMACAO else 0L

        raiz.isEnabled = true
        raiz.animate().cancel()
        raiz.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationZ(0f)
            .setDuration(duracao)
            .setInterpolator(CURVA_EASE_OUT)
            .start()

        destaques.forEach { destaque ->
            destaque.animate().cancel()
            destaque.animate().alpha(0f).setDuration(duracao).start()
        }
    }
}
