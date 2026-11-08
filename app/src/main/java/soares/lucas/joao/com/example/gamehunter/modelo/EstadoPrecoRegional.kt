package soares.lucas.joao.com.example.gamehunter.modelo

// Ausência de ofertas, falha de rede e preço zero são estados diferentes.
sealed interface EstadoPrecoRegional {
    data object Carregando : EstadoPrecoRegional
    data class Disponivel(val valor: Double) : EstadoPrecoRegional
    data object SemOfertas : EstadoPrecoRegional
    data class Falha(val mensagem: Int) : EstadoPrecoRegional
}
