package soares.lucas.joao.com.example.gamehunter.dados

import soares.lucas.joao.com.example.gamehunter.modelo.Genero
import soares.lucas.joao.com.example.gamehunter.modelo.Fileira
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Loja
import soares.lucas.joao.com.example.gamehunter.modelo.Oferta
import soares.lucas.joao.com.example.gamehunter.modelo.Plataforma

// Implementação PROVISÓRIA do RepositorioDeJogos, com dados apenas DEMONSTRATIVOS
// (os mesmos do protótipo no Figma), para as telas funcionarem antes da integração
// com as APIs. Nada aqui acessa a internet ou um banco de dados.
// A "Média hoje" de cada jogo é calculada a partir das ofertas, por isso os preços
// das lojas foram escolhidos para dar a mesma média mostrada no Figma.
// TODO (Backend/Database): criar a implementação real (CheapShark, RAWG/IGDB...) em
//  outra classe e trocá-la no objeto Repositorios. Esta classe pode então ser apagada
object RepositorioDeJogosDeExemplo : RepositorioDeJogos {

    // Catálogo com todos os jogos de exemplo
    private val jogos: List<Jogo> = listOf(
        Jogo(
            id = 1, titulo = "Elden Ring", capaUrl = null, nota = 9.6, precoLancamento = 249.90,
            plataformas = listOf(Plataforma.PS5, Plataforma.PC, Plataforma.PS4, Plataforma.XBOX_SERIES, Plataforma.XBOX_ONE),
            genero = "RPG de ação", desenvolvedora = "FromSoftware", anoLancamento = 2022,
            resumo = "Em um mundo aberto sombrio, você é um Maculado em busca dos fragmentos do Anel Prístino. Explore, enfrente chefes brutais e descubra segredos a cada canto.",
            precoEmDolar = 69.99,
            ofertas = ofertas(Loja.NUUVEM to 139.90, Loja.STEAM to 179.90, Loja.PLAYSTATION_STORE to 219.90)
        ),
        Jogo(
            id = 2, titulo = "Grand Theft Auto V", capaUrl = null, nota = 9.5, precoLancamento = 199.90,
            plataformas = listOf(Plataforma.PC, Plataforma.PS5, Plataforma.PS4, Plataforma.PS3, Plataforma.XBOX_SERIES, Plataforma.XBOX_ONE, Plataforma.XBOX_360),
            genero = "Ação e aventura", desenvolvedora = "Rockstar Games", anoLancamento = 2013,
            resumo = "Em Los Santos, três criminosos muito diferentes arriscam tudo em uma série de assaltos ousados. Explore um enorme mundo aberto, sozinho ou online.",
            precoEmDolar = 29.99,
            ofertas = ofertas(Loja.EPIC_GAMES to 39.90, Loja.STEAM to 49.90, Loja.MICROSOFT_STORE to 69.90, Loja.PLAYSTATION_STORE to 79.90)
        ),
        Jogo(
            id = 3, titulo = "Minecraft", capaUrl = null, nota = 9.3, precoLancamento = 99.90,
            plataformas = listOf(Plataforma.PC, Plataforma.SWITCH, Plataforma.PS4, Plataforma.XBOX_ONE, Plataforma.PS3, Plataforma.XBOX_360),
            genero = "Sandbox", desenvolvedora = "Mojang Studios", anoLancamento = 2011,
            resumo = "Construa, explore e sobreviva em um mundo feito de blocos e gerado aleatoriamente. Jogue no modo criativo ou enfrente criaturas no modo sobrevivência.",
            precoEmDolar = 29.99,
            ofertas = ofertas(Loja.MICROSOFT_STORE to 99.90, Loja.PLAYSTATION_STORE to 99.90, Loja.NINTENDO_ESHOP to 99.90)
        ),
        Jogo(
            id = 4, titulo = "Red Dead Redemption 2", capaUrl = null, nota = 9.7, precoLancamento = 249.90,
            plataformas = listOf(Plataforma.PC, Plataforma.PS4, Plataforma.XBOX_ONE),
            genero = "Ação e aventura", desenvolvedora = "Rockstar Games", anoLancamento = 2018,
            resumo = "Em 1899, Arthur Morgan e a gangue de Dutch van der Linde fogem pelo Velho Oeste americano enquanto a era dos fora da lei chega ao fim.",
            precoEmDolar = 59.99,
            ofertas = ofertas(Loja.STEAM to 89.90, Loja.EPIC_GAMES to 119.90, Loja.MICROSOFT_STORE to 119.90, Loja.PLAYSTATION_STORE to 149.90)
        ),
        Jogo(
            id = 5, titulo = "The Witcher 3: Wild Hunt", capaUrl = null, nota = 9.7, precoLancamento = 119.90,
            plataformas = listOf(Plataforma.PC, Plataforma.PS5, Plataforma.PS4, Plataforma.XBOX_SERIES, Plataforma.XBOX_ONE, Plataforma.SWITCH),
            genero = "RPG", desenvolvedora = "CD Projekt Red", anoLancamento = 2015,
            resumo = "Como o bruxo Geralt de Rívia, cace monstros e procure a jovem Ciri em um continente devastado pela guerra, cheio de escolhas com consequências.",
            precoEmDolar = 39.99,
            ofertas = ofertas(Loja.NUUVEM to 29.90, Loja.STEAM to 49.90, Loja.PLAYSTATION_STORE to 69.90)
        ),
        Jogo(
            id = 6, titulo = "Cyberpunk 2077", capaUrl = null, nota = 8.6, precoLancamento = 249.90,
            plataformas = listOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX_SERIES),
            genero = "RPG de ação", desenvolvedora = "CD Projekt Red", anoLancamento = 2020,
            resumo = "Em Night City, uma metrópole obcecada por poder e modificações corporais, viva como V, um mercenário em busca de um implante que garante a imortalidade.",
            precoEmDolar = 59.99,
            ofertas = ofertas(Loja.NUUVEM to 99.90, Loja.STEAM to 129.90, Loja.PLAYSTATION_STORE to 159.90)
        ),
        Jogo(
            id = 7, titulo = "Hogwarts Legacy", capaUrl = null, nota = 8.9, precoLancamento = 249.90,
            plataformas = listOf(Plataforma.PC, Plataforma.PS5, Plataforma.PS4, Plataforma.XBOX_SERIES, Plataforma.XBOX_ONE, Plataforma.SWITCH),
            genero = "RPG de ação", desenvolvedora = "Avalanche Software", anoLancamento = 2023,
            resumo = "Viva como um estudante de Hogwarts no século XIX: aprenda feitiços, prepare poções e descubra um segredo antigo do mundo bruxo.",
            precoEmDolar = 59.99,
            ofertas = ofertas(Loja.STEAM to 119.90, Loja.EPIC_GAMES to 149.90, Loja.PLAYSTATION_STORE to 179.90)
        ),
        Jogo(
            id = 8, titulo = "Baldur's Gate 3", capaUrl = null, nota = 9.6, precoLancamento = 199.90,
            plataformas = listOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX_SERIES),
            genero = "RPG", desenvolvedora = "Larian Studios", anoLancamento = 2023,
            resumo = "Reúna um grupo de aventureiros nos Reinos Esquecidos e decida o destino de todos em um RPG por turnos inspirado em Dungeons & Dragons.",
            precoEmDolar = 59.99,
            ofertas = ofertas(Loja.STEAM to 199.90, Loja.PLAYSTATION_STORE to 199.90)
        ),
        Jogo(
            id = 9, titulo = "Persona 5 Royal", capaUrl = null, nota = 9.4, precoLancamento = 249.90,
            plataformas = listOf(Plataforma.PS4, Plataforma.PS5, Plataforma.PC, Plataforma.SWITCH, Plataforma.XBOX_SERIES),
            genero = "RPG", desenvolvedora = "Atlus", anoLancamento = 2019,
            resumo = "De dia, viva a rotina de um estudante em Tóquio; à noite, invada o mundo cognitivo como um Ladrão Fantasma para mudar o coração de adultos corruptos.",
            precoEmDolar = 59.99,
            ofertas = ofertas(Loja.STEAM to 89.90, Loja.MICROSOFT_STORE to 119.90, Loja.PLAYSTATION_STORE to 149.90)
        ),
        Jogo(
            id = 10, titulo = "Dark Souls III", capaUrl = null, nota = 9.0, precoLancamento = 199.90,
            plataformas = listOf(Plataforma.PC, Plataforma.PS4, Plataforma.XBOX_ONE),
            genero = "RPG de ação", desenvolvedora = "FromSoftware", anoLancamento = 2016,
            resumo = "Em um reino em ruínas, enfrente inimigos implacáveis e chefes gigantescos enquanto as chamas da Era do Fogo se apagam.",
            precoEmDolar = 59.99,
            ofertas = ofertas(Loja.NUUVEM to 39.90, Loja.STEAM to 59.90, Loja.PLAYSTATION_STORE to 79.90)
        ),
        Jogo(
            id = 11, titulo = "Sekiro: Shadows Die Twice", capaUrl = null, nota = 9.0, precoLancamento = 249.90,
            plataformas = listOf(Plataforma.PC, Plataforma.PS4, Plataforma.XBOX_ONE),
            genero = "Ação e aventura", desenvolvedora = "FromSoftware", anoLancamento = 2019,
            resumo = "No Japão do período Sengoku, um shinobi busca vingança e o resgate de seu jovem senhor em combates de espada precisos e desafiadores.",
            precoEmDolar = 59.99,
            ofertas = ofertas(Loja.STEAM to 99.90, Loja.MICROSOFT_STORE to 129.90, Loja.PLAYSTATION_STORE to 159.90)
        ),
        Jogo(
            id = 12, titulo = "Final Fantasy VII Remake", capaUrl = null, nota = 9.0, precoLancamento = 249.90,
            plataformas = listOf(Plataforma.PS4, Plataforma.PS5, Plataforma.PC),
            genero = "RPG de ação", desenvolvedora = "Square Enix", anoLancamento = 2020,
            resumo = "Cloud Strife e o grupo Avalanche lutam contra a corporação Shinra na cidade de Midgar, em uma recriação do clássico de 1997.",
            precoEmDolar = 69.99,
            ofertas = ofertas(Loja.EPIC_GAMES to 99.90, Loja.STEAM to 129.90, Loja.PLAYSTATION_STORE to 159.90)
        ),
        Jogo(
            id = 13, titulo = "Bloodborne", capaUrl = null, nota = 9.3, precoLancamento = 199.90,
            plataformas = listOf(Plataforma.PS4),
            genero = "RPG de ação", desenvolvedora = "FromSoftware", anoLancamento = 2015,
            resumo = "Na cidade gótica de Yharnam, assolada por uma doença misteriosa, cace criaturas de pesadelo e descubra os segredos por trás da praga.",
            precoEmDolar = 19.99,
            ofertas = ofertas(Loja.PLAYSTATION_STORE to 99.90)
        ),
        Jogo(
            id = 14, titulo = "Disco Elysium", capaUrl = null, nota = 9.1, precoLancamento = 99.90,
            plataformas = listOf(Plataforma.PC),
            genero = "RPG", desenvolvedora = "ZA/UM", anoLancamento = 2019,
            resumo = "Como um detetive com amnésia, investigue um assassinato na cidade de Revachol em um RPG movido a diálogos e escolhas.",
            precoEmDolar = 39.99,
            ofertas = ofertas(Loja.NUUVEM to 39.90, Loja.STEAM to 59.90, Loja.EPIC_GAMES to 79.90)
        ),
        Jogo(
            id = 15, titulo = "God of War (2018)", capaUrl = null, nota = 9.5, precoLancamento = 199.90,
            plataformas = listOf(Plataforma.PS4, Plataforma.PS5, Plataforma.PC),
            genero = "Ação e aventura", desenvolvedora = "Santa Monica Studio", anoLancamento = 2018,
            resumo = "Kratos agora vive nas terras nórdicas e precisa guiar o filho, Atreus, em uma jornada perigosa para espalhar as cinzas da esposa.",
            precoEmDolar = 49.99,
            ofertas = ofertas(Loja.STEAM to 49.90, Loja.EPIC_GAMES to 69.90, Loja.PLAYSTATION_STORE to 89.90)
        ),
        Jogo(
            id = 16, titulo = "God of War Ragnarök", capaUrl = null, nota = 9.4, precoLancamento = 299.90,
            plataformas = listOf(Plataforma.PS5, Plataforma.PS4, Plataforma.PC),
            genero = "Ação e aventura", desenvolvedora = "Santa Monica Studio", anoLancamento = 2022,
            resumo = "Com o Ragnarök se aproximando, Kratos e Atreus viajam pelos Nove Reinos em busca de respostas enquanto Asgard se prepara para a guerra.",
            precoEmDolar = 59.99,
            ofertas = ofertas(Loja.STEAM to 169.90, Loja.EPIC_GAMES to 199.90, Loja.PLAYSTATION_STORE to 229.90)
        ),
        Jogo(
            id = 17, titulo = "God of War II", capaUrl = null, nota = 9.3, precoLancamento = 149.90,
            plataformas = listOf(Plataforma.PS2, Plataforma.PS3),
            genero = "Ação e aventura", desenvolvedora = "Santa Monica Studio", anoLancamento = 2007,
            resumo = "Traído pelos deuses do Olimpo, Kratos procura as Irmãs do Destino para mudar o passado e se vingar de Zeus.",
            precoEmDolar = 19.99,
            ofertas = ofertas(Loja.PLAYSTATION_STORE to 59.90)
        ),
        Jogo(
            id = 18, titulo = "God of War III Remastered", capaUrl = null, nota = 9.0, precoLancamento = 199.90,
            plataformas = listOf(Plataforma.PS4),
            genero = "Ação e aventura", desenvolvedora = "Santa Monica Studio", anoLancamento = 2015,
            resumo = "Kratos escala o Monte Olimpo para destruir os deuses gregos, nesta versão remasterizada do capítulo final da trilogia original.",
            precoEmDolar = 19.99,
            ofertas = ofertas(Loja.PLAYSTATION_STORE to 79.90)
        ),
        Jogo(
            id = 19, titulo = "God of War: Ascension", capaUrl = null, nota = 7.5, precoLancamento = 199.90,
            plataformas = listOf(Plataforma.PS3),
            genero = "Ação e aventura", desenvolvedora = "Santa Monica Studio", anoLancamento = 2013,
            resumo = "Antes da saga original, Kratos tenta se libertar do juramento feito a Ares enquanto é perseguido pelas Fúrias.",
            precoEmDolar = 19.99,
            ofertas = ofertas(Loja.PLAYSTATION_STORE to 49.90)
        ),
        Jogo(
            id = 20, titulo = "God of War Collection", capaUrl = null, nota = 9.0, precoLancamento = 199.90,
            plataformas = listOf(Plataforma.PS3),
            genero = "Ação e aventura", desenvolvedora = "Santa Monica Studio", anoLancamento = 2009,
            resumo = "Os dois primeiros jogos da série, remasterizados em alta definição para o PlayStation 3.",
            precoEmDolar = 29.99,
            ofertas = ofertas(Loja.PLAYSTATION_STORE to 69.90)
        )
    )

    // Funções do RepositorioDeJogos, respondidas com os dados de exemplo:
    // - fileiras: montadas a partir dos ids dos jogos do catálogo
    // - buscarPorTitulo: jogos cujo nome contém o texto, sem diferenciar maiúsculas
    // - jogosDaFileira: os jogos da fileira com aquele título (lista vazia se não existir)
    override suspend fun fileiras(generos: Set<Genero>): List<Fileira> {
        return listOf(
            Fileira(
                titulo = "Mais vendidos",
                subtitulo = "Os queridinhos da comunidade",
                jogos = jogosComIds(1, 2, 3, 4, 5, 6, 7)
            ),
            Fileira(
                titulo = "De acordo com o que você gosta",
                subtitulo = "RPG e ação, com base no seu perfil",
                jogos = jogosComIds(8, 9, 10, 11, 12, 13, 14)
            )
        )
    }

    override suspend fun buscarPorTitulo(texto: String): List<Jogo> {
        return jogos.filter { it.titulo.contains(texto, ignoreCase = true) }
    }

    override suspend fun jogosDaFileira(tituloDaFileira: String, generos: Set<Genero>): List<Jogo> {
        return fileiras(generos).find { it.titulo == tituloDaFileira }?.jogos.orEmpty()
    }

    // Funções auxiliares para montar os dados acima com menos repetição:
    // jogosComIds procura os jogos no catálogo e ofertas transforma pares
    // "loja to preço" em uma lista de Oferta
    private fun jogosComIds(vararg ids: Int): List<Jogo> {
        return ids.toList().mapNotNull { id -> jogos.find { it.id == id } }
    }

    private fun ofertas(vararg lojasEPrecos: Pair<Loja, Double>): List<Oferta> {
        return lojasEPrecos.map { (loja, preco) -> Oferta(loja, preco) }
    }
}
