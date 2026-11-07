package soares.lucas.joao.com.example.gamehunter.dados

// PONTO ÚNICO DE TROCA dos repositórios usados pelo app. As telas pegam os dados
// sempre por aqui (ex.: Repositorios.jogos.buscarPorTitulo("Elden Ring")).
// TODO (Backend/Database): quando a implementação real existir, troque apenas a linha
//  abaixo, por exemplo: val jogos: RepositorioDeJogos = RepositorioDeJogosApi()
object Repositorios {
    val jogos: RepositorioDeJogos = RepositorioDeJogosDeExemplo
}
