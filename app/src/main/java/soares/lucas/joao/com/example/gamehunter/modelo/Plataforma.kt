package soares.lucas.joao.com.example.gamehunter.modelo

// Plataformas de jogo suportadas. "nome" aparece nos chips de filtro e
// "sigla" nas etiquetas pequenas dos cards e dos detalhes do jogo
enum class Plataforma(val nome: String, val sigla: String) {
    PC("PC", "PC"),
    PS5("PS5", "PS5"),
    PS4("PS4", "PS4"),
    PS3("PS3", "PS3"),
    PS2("PS2", "PS2"),
    XBOX_SERIES("Xbox Series", "XSX"),
    XBOX_ONE("Xbox One", "XONE"),
    XBOX_360("Xbox 360", "X360"),
    SWITCH("Switch", "SWITCH"),
    SWITCH_2("Switch 2", "SWITCH 2")
}
