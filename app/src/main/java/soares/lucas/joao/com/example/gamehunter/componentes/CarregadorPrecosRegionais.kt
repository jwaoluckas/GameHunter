package soares.lucas.joao.com.example.gamehunter.componentes

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import soares.lucas.joao.com.example.gamehunter.dados.RepositorioDeJogos
import soares.lucas.joao.com.example.gamehunter.mensagemErroItad
import soares.lucas.joao.com.example.gamehunter.modelo.EstadoPrecoRegional
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda

// Uma região pode falhar sem apagar as demais. Marcar novamente uma moeda que
// falhou permite nova tentativa. O escopo pertence ao diálogo, que o cancela ao fechar.
internal class CarregadorPrecosRegionais(
    private val escopo: CoroutineScope,
    private val repositorio: RepositorioDeJogos,
    private val idItad: String?,
    private val aoAtualizar: (Map<Moeda, EstadoPrecoRegional>) -> Unit
) {
    private val estados = mutableMapOf<Moeda, EstadoPrecoRegional>()

    fun carregar(moedas: List<Moeda>) {
        moedas.distinct().forEach { moeda ->
            val atual = estados[moeda]
            if (atual != null && atual !is EstadoPrecoRegional.Falha) return@forEach
            if (idItad == null) {
                estados[moeda] = EstadoPrecoRegional.SemOfertas
                return@forEach
            }
            estados[moeda] = EstadoPrecoRegional.Carregando
            escopo.launch {
                val estado = try {
                    repositorio.precoRegional(idItad, moeda)?.let { EstadoPrecoRegional.Disponivel(it) }
                        ?: EstadoPrecoRegional.SemOfertas
                } catch (erro: Exception) {
                    if (erro is CancellationException) throw erro
                    EstadoPrecoRegional.Falha(mensagemErroItad(erro))
                }
                estados[moeda] = estado
                aoAtualizar(estados.toMap())
            }
        }
        aoAtualizar(estados.toMap())
    }
}
