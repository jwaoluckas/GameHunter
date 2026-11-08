package soares.lucas.joao.com.example.gamehunter.modais

import android.content.Context
import com.bumptech.glide.Glide
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.adaptadores.MoedaAdapter
import soares.lucas.joao.com.example.gamehunter.componentes.GrupoDeChips
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ModalCompararMoedasBinding
import soares.lucas.joao.com.example.gamehunter.exibirComoFolhaInferior
import soares.lucas.joao.com.example.gamehunter.formatarValorNaMoeda
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda
import soares.lucas.joao.com.example.gamehunter.preencherEtiquetasDePlataforma
import kotlinx.coroutines.cancel
import soares.lucas.joao.com.example.gamehunter.componentes.CarregadorPrecosRegionais
import soares.lucas.joao.com.example.gamehunter.dados.Repositorios
import soares.lucas.joao.com.example.gamehunter.modelo.EstadoPrecoRegional

// Modal COMPARAR MOEDAS (RF12): consulta preços do mesmo jogo por país.
// Usa a média de ofertas em cada moeda, sem conversões cambiais fictícias. As moedas escolhidas ficam salvas nas preferências
// ao tocar em "Aplicar moedas", e a função aoAplicar avisa quem abriu o modal
class ModalCompararMoedas(
    private val contexto: Context,
    private val jogo: Jogo,
    private val aoAplicar: () -> Unit = {}
) {

    private val binding = ModalCompararMoedasBinding.inflate(LayoutInflater.from(contexto))
    private val preferencias = PreferenciasUsuario(contexto)
    private val moedaPadrao = preferencias.moedaPadrao

    private val moedaAdapter: MoedaAdapter = MoedaAdapter(moedaPadrao, preferencias.moedasSelecionadas) { tentarNovamente(it) }
    private val escopo = contexto.criarEscopoDoModal()
    private val carregador = CarregadorPrecosRegionais(escopo, Repositorios.jogos(contexto), jogo.idItad) { estados ->
        moedaAdapter.atualizarPrecos(estados)
        atualizarDestaque(estados)
        val brasil = estados[Moeda.BRL] as? EstadoPrecoRegional.Disponivel
        if (brasil != null) binding.txtMediaHoje.text = contexto.getString(
            R.string.moedas_media_hoje, formatarValorNaMoeda(contexto, Moeda.BRL, brasil.valor)
        )
    }

    private fun tentarNovamente(moeda: Moeda) {
        carregador.carregar(listOf(moeda))
    }

    // Chips com todas as moedas (USD, BRL, EUR...). Ao marcar ou desmarcar, a lista
    // de preços já é atualizada, mesmo antes de aplicar. Pelo menos uma moeda
    // precisa continuar marcada
    private val grupoMoedas = GrupoDeChips(
        binding.flowMoedas,
        Moeda.entries.map { it.name },
        selecaoMultipla = true,
        minimoUmSelecionado = true,
        aoMudar = {
            moedaAdapter.atualizarMoedas(moedasMarcadas())
            carregador.carregar(moedasMarcadas())
        }
    )

    // Preenche o resumo do jogo, a lista de moedas e o destaque, cria o AlertDialog
    // com o layout do modal, configura os botões e exibe o modal na parte de baixo
    fun exibir() {
        preencherJogo()

        grupoMoedas.selecionar(preferencias.moedasSelecionadas.map { it.ordinal })
        binding.rvMoedas.layoutManager = LinearLayoutManager(contexto)
        binding.rvMoedas.adapter = moedaAdapter

        binding.txtDestaque.setText(R.string.moedas_consultando)

        val dialogo = AlertDialog.Builder(contexto)
            .setView(binding.root)
            .create()

        dialogo.setOnDismissListener { escopo.cancel() }
        binding.btnFechar.setOnClickListener { dialogo.dismiss() }
        binding.btnAplicar.setOnClickListener {
            preferencias.salvarMoedasSelecionadas(moedasMarcadas())
            aoAplicar()
            dialogo.dismiss()
        }

        exibirComoFolhaInferior(dialogo)
        carregador.carregar(moedasMarcadas())
    }

    // Moedas dos chips marcados, na ordem do enum Moeda
    private fun moedasMarcadas(): List<Moeda> = grupoMoedas.selecionados().map { Moeda.entries[it] }

    // Caixa com o jogo: capa (degradê enquanto não há imagem), título, plataformas
    // e a média de preço de hoje
    private fun preencherJogo() {
        binding.frameCapa.clipToOutline = true
        Glide.with(binding.imgCapa).load(jogo.capaUrl).centerCrop().into(binding.imgCapa)
        binding.txtTituloJogo.text = jogo.titulo
        preencherEtiquetasDePlataforma(
            listOf(binding.txtPlataforma1, binding.txtPlataforma2, binding.txtPlataforma3),
            jogo.plataformas
        )
        binding.txtMediaHoje.text = contexto.getString(
            R.string.moedas_media_hoje,
            formatarValorNaMoeda(contexto, Moeda.BRL, jogo.precoMedioHoje)
        )
    }

    private fun atualizarDestaque(estados: Map<Moeda, EstadoPrecoRegional>) {
        val selecionados = moedasMarcadas().mapNotNull { estados[it] }
        val falha = selecionados.filterIsInstance<EstadoPrecoRegional.Falha>().firstOrNull()
        binding.txtDestaque.text = when {
            selecionados.any { it is EstadoPrecoRegional.Carregando } -> contexto.getString(R.string.moedas_consultando)
            falha != null -> contexto.getString(falha.mensagem)
            else -> contexto.getString(R.string.moedas_destaque_regional)
        }
    }
}
