package soares.lucas.joao.com.example.gamehunter.modais

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.adaptadores.MoedaAdapter
import soares.lucas.joao.com.example.gamehunter.componentes.GrupoDeChips
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ModalCompararMoedasBinding
import soares.lucas.joao.com.example.gamehunter.exibirComoFolhaInferior
import soares.lucas.joao.com.example.gamehunter.formatarPreco
import soares.lucas.joao.com.example.gamehunter.formatarValorNaMoeda
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda
import soares.lucas.joao.com.example.gamehunter.preencherEtiquetasDePlataforma
import kotlin.math.abs

// Modal COMPARAR MOEDAS (RF12): mostra o preço do jogo em várias moedas, convertido
// para a moeda padrão do usuário. As moedas escolhidas ficam salvas nas preferências
// ao tocar em "Aplicar moedas", e a função aoAplicar avisa quem abriu o modal
class ModalCompararMoedas(
    private val contexto: Context,
    private val jogo: Jogo,
    private val aoAplicar: () -> Unit = {}
) {

    private val binding = ModalCompararMoedasBinding.inflate(LayoutInflater.from(contexto))
    private val preferencias = PreferenciasUsuario(contexto)
    private val moedaPadrao = preferencias.moedaPadrao

    private val moedaAdapter = MoedaAdapter(jogo, moedaPadrao, preferencias.moedasSelecionadas)

    // Chips com todas as moedas (USD, BRL, EUR...). Ao marcar ou desmarcar, a lista
    // de preços já é atualizada, mesmo antes de aplicar. Pelo menos uma moeda
    // precisa continuar marcada
    private val grupoMoedas = GrupoDeChips(
        binding.flowMoedas,
        Moeda.entries.map { it.name },
        selecaoMultipla = true,
        minimoUmSelecionado = true,
        aoMudar = { moedaAdapter.atualizarMoedas(moedasMarcadas()) }
    )

    // Preenche o resumo do jogo, a lista de moedas e o destaque, cria o AlertDialog
    // com o layout do modal, configura os botões e exibe o modal na parte de baixo
    fun exibir() {
        preencherJogo()

        grupoMoedas.selecionar(preferencias.moedasSelecionadas.map { it.ordinal })
        binding.rvMoedas.layoutManager = LinearLayoutManager(contexto)
        binding.rvMoedas.adapter = moedaAdapter

        binding.txtDestaque.text = textoDoDestaque()

        val dialogo = AlertDialog.Builder(contexto)
            .setView(binding.root)
            .create()

        binding.btnFechar.setOnClickListener { dialogo.dismiss() }
        binding.btnAplicar.setOnClickListener {
            preferencias.salvarMoedasSelecionadas(moedasMarcadas())
            aoAplicar()
            dialogo.dismiss()
        }

        exibirComoFolhaInferior(dialogo)
    }

    // Moedas dos chips marcados, na ordem do enum Moeda
    private fun moedasMarcadas(): List<Moeda> = grupoMoedas.selecionados().map { Moeda.entries[it] }

    // Caixa com o jogo: capa (degradê enquanto não há imagem), título, plataformas
    // e a média de preço de hoje
    private fun preencherJogo() {
        binding.frameCapa.clipToOutline = true
        binding.txtTituloJogo.text = jogo.titulo
        preencherEtiquetasDePlataforma(
            listOf(binding.txtPlataforma1, binding.txtPlataforma2, binding.txtPlataforma3),
            jogo.plataformas
        )
        binding.txtMediaHoje.text = contexto.getString(
            R.string.moedas_media_hoje,
            contexto.getString(R.string.formato_preco, formatarPreco(jogo.precoMedioHoje))
        )
    }

    // Diferença entre o preço nos EUA (convertido para reais) e a média no Brasil,
    // mostrada na moeda padrão do usuário. O texto muda se no Brasil for mais
    // barato ("a menos") ou mais caro ("a mais")
    private fun textoDoDestaque(): String {
        val precoEua = Moeda.USD.converter(Moeda.USD.precoDoJogo(jogo), Moeda.BRL)
        val diferencaEmReais = precoEua - jogo.precoMedioHoje
        val diferenca = Moeda.BRL.converter(abs(diferencaEmReais), moedaPadrao)
        val valor = formatarValorNaMoeda(contexto, moedaPadrao, diferenca)

        val texto = if (diferencaEmReais >= 0) R.string.moedas_destaque_menos else R.string.moedas_destaque_mais
        return contexto.getString(texto, valor)
    }
}
