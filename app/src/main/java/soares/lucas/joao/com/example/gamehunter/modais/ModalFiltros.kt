package soares.lucas.joao.com.example.gamehunter.modais

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.componentes.GrupoDeChips
import soares.lucas.joao.com.example.gamehunter.databinding.ModalFiltrosBinding
import soares.lucas.joao.com.example.gamehunter.exibirComoFolhaInferior
import soares.lucas.joao.com.example.gamehunter.modelo.Filtros
import soares.lucas.joao.com.example.gamehunter.modelo.Loja
import soares.lucas.joao.com.example.gamehunter.modelo.Ordenacao
import soares.lucas.joao.com.example.gamehunter.modelo.Genero

// Modal de FILTROS E ORDENAÇÃO (RF05), usado na tela Principal e no Resultado de
// pesquisas. Abre com os filtros que já estão em uso e, ao tocar em "Aplicar filtros",
// devolve os novos filtros para a tela pela função aoAplicar
class ModalFiltros(
    private val contexto: Context,
    private val filtrosAtuais: Filtros,
    private val aoAplicar: (Filtros) -> Unit
) {

    private val binding = ModalFiltrosBinding.inflate(LayoutInflater.from(contexto))

    // Um grupo de chips para cada seção do modal. Ordenação e preço máximo aceitam só
    // uma opção; gêneros e lojas aceitam várias. "Somente em promoção" é um único
    // chip que liga e desliga (seleção múltipla com uma opção só).
    // Os textos dos preços máximos são "Até R$ 50", "Até R$ 100", "Até R$ 200" e
    // "Qualquer valor" (a última opção, que significa sem limite)
    private val grupoOrdenacao = GrupoDeChips(
        binding.flowOrdenacao,
        Ordenacao.entries.map { contexto.getString(it.texto) },
        selecaoMultipla = false
    )
    private val grupoGeneros = GrupoDeChips(
        binding.flowPlataformas,
        Genero.entries.map { contexto.getString(it.texto) },
        selecaoMultipla = true
    )
    private val grupoLojas = GrupoDeChips(
        binding.flowLojas,
        Loja.entries.map { it.nome },
        selecaoMultipla = true
    )
    private val grupoPrecoMaximo = GrupoDeChips(
        binding.flowPrecoMaximo,
        Filtros.PRECOS_MAXIMOS.map { contexto.getString(R.string.filtros_ate_preco, it) } +
            contexto.getString(R.string.filtros_qualquer_valor),
        selecaoMultipla = false
    )
    private val grupoMostrar = GrupoDeChips(
        binding.flowMostrar,
        listOf(contexto.getString(R.string.filtros_somente_promocao)),
        selecaoMultipla = true
    )

    // Marca os chips de acordo com os filtros atuais, cria o AlertDialog com o layout
    // do modal, configura os botões e exibe o modal na parte de baixo da tela
    fun exibir() {
        mostrarFiltros(filtrosAtuais)

        val dialogo = AlertDialog.Builder(contexto)
            .setView(binding.root)
            .create()

        binding.btnFechar.setOnClickListener { dialogo.dismiss() }
        binding.btnLimpar.setOnClickListener { mostrarFiltros(Filtros()) }
        binding.btnAplicar.setOnClickListener {
            aoAplicar(lerFiltros())
            dialogo.dismiss()
        }

        exibirComoFolhaInferior(dialogo)
    }

    // Filtros -> chips: converte cada valor para a posição do chip correspondente.
    // Sem preço máximo (null), marca o último chip ("Qualquer valor")
    private fun mostrarFiltros(filtros: Filtros) {
        grupoOrdenacao.selecionar(filtros.ordenacao.ordinal)
        grupoGeneros.selecionar(filtros.generos.map { it.ordinal })
        grupoLojas.selecionar(filtros.lojas.map { it.ordinal })

        val precoMaximo = filtros.precoMaximo
        val indicePreco = if (precoMaximo == null) -1 else Filtros.PRECOS_MAXIMOS.indexOf(precoMaximo)
        grupoPrecoMaximo.selecionar(if (indicePreco >= 0) indicePreco else Filtros.PRECOS_MAXIMOS.size)

        grupoMostrar.selecionar(if (filtros.somentePromocao) listOf(0) else emptyList())
    }

    // Chips -> Filtros: faz o caminho contrário, transformando as posições marcadas
    // de volta em ordenação, gêneros, lojas, preço máximo e promoção
    private fun lerFiltros(): Filtros {
        val indicePreco = grupoPrecoMaximo.selecionado()

        return Filtros(
            ordenacao = Ordenacao.entries[grupoOrdenacao.selecionado()],
            generos = grupoGeneros.selecionados().map { Genero.entries[it] }.toSet(),
            lojas = grupoLojas.selecionados().map { Loja.entries[it] }.toSet(),
            precoMaximo = Filtros.PRECOS_MAXIMOS.getOrNull(indicePreco),
            somentePromocao = grupoMostrar.selecionados().isNotEmpty()
        )
    }
}
