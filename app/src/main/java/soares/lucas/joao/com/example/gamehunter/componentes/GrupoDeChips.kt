package soares.lucas.joao.com.example.gamehunter.componentes

import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.helper.widget.Flow
import soares.lucas.joao.com.example.gamehunter.adicionarAoFlow
import soares.lucas.joao.com.example.gamehunter.databinding.ItemChipBinding

// Grupo de chips colocados dentro de um Flow (que quebra a linha quando os chips não
// cabem), usado nos modais e nas Configurações. Pode funcionar de dois jeitos:
// - seleção única (selecaoMultipla = false): tocar em um chip desmarca os outros
// - seleção múltipla: cada toque marca/desmarca só aquele chip. Com
//   minimoUmSelecionado = true, o último chip marcado não pode ser desmarcado
// Os chips são identificados pela posição (índice) na lista de textos
class GrupoDeChips(
    flow: Flow,
    textos: List<String>,
    private val selecaoMultipla: Boolean,
    private val minimoUmSelecionado: Boolean = false,
    private val aoMudar: () -> Unit = {}
) {

    private val chips: List<TextView>

    // Cria um chip (layout item_chip) para cada texto e o coloca no Flow
    init {
        val inflater = LayoutInflater.from(flow.context)
        chips = textos.mapIndexed { indice, texto ->
            val chip = ItemChipBinding.inflate(inflater).root
            chip.text = texto
            chip.setOnClickListener { tocarNoChip(indice) }
            adicionarAoFlow(flow, chip)
            chip
        }
    }

    // Leitura da seleção: todos os índices marcados ou o primeiro marcado (-1 se nenhum)
    fun selecionados(): List<Int> = chips.indices.filter { chips[it].isSelected }

    fun selecionado(): Int = chips.indexOfFirst { it.isSelected }

    // Marca exatamente os chips informados e desmarca os demais
    fun selecionar(indices: Collection<Int>) {
        chips.forEachIndexed { indice, chip -> chip.isSelected = indice in indices }
    }

    fun selecionar(indice: Int) = selecionar(listOf(indice))

    // Reação ao toque, de acordo com o tipo de seleção do grupo. No fim avisa
    // quem criou o grupo (ex.: para atualizar a lista de moedas na hora)
    private fun tocarNoChip(indice: Int) {
        val chip = chips[indice]

        if (!selecaoMultipla) {
            selecionar(indice)
        } else if (chip.isSelected && minimoUmSelecionado && selecionados().size == 1) {
            return
        } else {
            chip.isSelected = !chip.isSelected
        }

        aoMudar()
    }
}
