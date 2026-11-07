# GameHunter

Aplicativo Android (Kotlin) para **buscar, comparar e acompanhar promoções de jogos digitais** em várias lojas (Steam, Epic Games, Nuuvem, PlayStation Store...) num só lugar. É o projeto final da disciplina **Programação para Dispositivos Móveis** (Engenharia de Software, IFPE – Campus Belo Jardim).

> **Status atual:** o **Frontend está pronto** e funciona com **dados de exemplo** (os mesmos do protótipo no Figma). Ainda **não existe** acesso à internet nem banco de dados. Essa parte fica com o **Backend/Database**. Este README explica onde e como encaixar essa parte sem precisar mexer nas telas.

---

## Sumário

1. [Divisão de responsabilidades](#1-divisão-de-responsabilidades)
2. [Como rodar o projeto](#2-como-rodar-o-projeto)
3. [Telas e navegação](#3-telas-e-navegação)
4. [Estrutura de pastas](#4-estrutura-de-pastas)
5. [**Para o Backend/Database: como conectar os dados reais**](#5-para-o-backenddatabase-como-conectar-os-dados-reais)
6. [Formato dos dados (modelos)](#6-formato-dos-dados-modelos)
7. [Lista de pendências (TODOs) por área](#7-lista-de-pendências-todos-por-área)
8. [Requisitos do professor: situação atual](#8-requisitos-do-professor-situação-atual)
9. [Convenções do código](#9-convenções-do-código)
10. [O que não vai para o GitHub (.gitignore)](#10-o-que-não-vai-para-o-github-gitignore)

---

## 1. Divisão de responsabilidades

| Área | O que inclui | Situação |
|---|---|---|
| **Frontend** | Telas (Activities, Fragment e modais), layouts XML, navegação, adapters, validação de formulários, formatação de preços | ✅ Pronto (com dados de exemplo) |
| **Backend** | Contas de usuário (login, cadastro, recuperar senha), integração com as APIs (CheapShark, RAWG/IGDB, câmbio), amizades, histórico de preços | ⏳ A fazer |
| **Database** | Persistência (Room/SQLite para favoritos; Firebase/API REST para contas e amigos) | ⏳ A fazer |

**Regra principal:** as telas **não sabem de onde os dados vêm**. Elas pedem tudo a uma interface (`RepositorioDeJogos`). Hoje quem responde é uma classe de exemplo; amanhã será a implementação real de vocês. Veja a [seção 5](#5-para-o-backenddatabase-como-conectar-os-dados-reais).

---

## 2. Como rodar o projeto

**Pré-requisitos**
- Android Studio recente (o projeto usa Android Gradle Plugin 9.3 e Gradle 9.5; o wrapper baixa o Gradle sozinho)
- JDK 11 ou superior (o que vem com o Android Studio já serve)
- Um emulador ou celular com Android 7.0 (API 24) ou superior

**Passos**
1. Clone o repositório:
   ```bash
   git clone https://github.com/jwaoluckas/GameHunter.git
   ```
2. Abra a pasta no Android Studio (**File → Open**) e espere o *Gradle Sync* terminar.
   - O arquivo `local.properties` (caminho do SDK) é criado automaticamente pelo Android Studio. Ele **não** vai para o GitHub.
3. Clique em **Run ▶** com um emulador aberto.

Pela linha de comando:
```bash
./gradlew assembleDebug        # gera o APK em app/build/outputs/apk/debug/
./gradlew lint                 # verifica problemas no código e nos layouts
```

**Login de teste:** como ainda não há Backend, **qualquer e-mail válido com qualquer senha preenchida** entra no app (ex.: `joao@email.com` / `12345678`).

---

## 3. Telas e navegação

O app usa **6 Activities + 1 Fragment** (requisito 3 do professor):

```
MainActivity (Login)
 ├── CadastroActivity
 ├── RecuperarSenhaActivity
 └── PrincipalActivity ──────────── contém o InicioFragment (busca, chips, fileiras)
      ├── menu de perfil (AlertDialog) → ConfiguracoesActivity
      ├── busca / "Ver tudo"         → ResultadoPesquisaActivity (grade de 2 colunas)
      └── modais (AlertDialog em formato de folha inferior):
           ├── ModalJogo             (detalhes, preços por moeda, lojas)
           ├── ModalCompararMoedas   (comparação do preço em várias moedas)
           └── ModalFiltros          (ordenação, plataformas, lojas, teto de preço)
```

| Tela | Arquivo Kotlin | Layout | Requisitos do documento |
|---|---|---|---|
| Login | `MainActivity.kt` | `activity_main.xml` | – |
| Cadastro | `CadastroActivity.kt` | `activity_cadastro.xml` | – |
| Recuperar senha | `RecuperarSenhaActivity.kt` | `activity_recuperar_senha.xml` | – |
| Principal | `PrincipalActivity.kt` + `InicioFragment.kt` | `activity_principal.xml`, `fragment_inicio.xml` | RF01, RF02, RF05 |
| Resultado de pesquisas | `ResultadoPesquisaActivity.kt` | `activity_resultado_pesquisa.xml` | RF02, RF05 |
| Detalhes do jogo | `modais/ModalJogo.kt` | `modal_jogo.xml` | RF03, RF04, RF09 |
| Comparar moedas | `modais/ModalCompararMoedas.kt` | `modal_comparar_moedas.xml` | RF12 |
| Filtros | `modais/ModalFiltros.kt` | `modal_filtros.xml` | RF05 |
| Configurações | `ConfiguracoesActivity.kt` | `activity_configuracoes.xml` | – |

Protótipo de referência: [Figma do GameHunter](https://www.figma.com/design/1YsULzEFrrQ0EMumAc1ikR/GameHunter).

---

## 4. Estrutura de pastas

Pacote: `app/src/main/java/soares/lucas/joao/com/example/gamehunter/`

```
gamehunter/
├── *Activity.kt, InicioFragment.kt   → telas
├── Utilitarios.kt                    → validação de campos, formatação de preço/nota, helpers de tela
├── adaptadores/                      → Adapters dos RecyclerViews (cards, fileiras, chips, ofertas, moedas)
├── componentes/GrupoDeChips.kt       → grupo de chips com seleção única ou múltipla (usado nos modais)
├── modais/                           → ModalJogo, ModalCompararMoedas, ModalFiltros
├── modelo/                           → classes de dados (Jogo, Oferta, Loja, Moeda, Filtros...)
└── dados/                            → ⭐ CAMADA DE DADOS (ponto de encaixe do Backend/Database)
    ├── RepositorioDeJogos.kt             interface: o "contrato" entre telas e dados
    ├── RepositorioDeJogosDeExemplo.kt    implementação PROVISÓRIA com dados de exemplo
    ├── Repositorios.kt                   lugar ÚNICO onde se escolhe qual implementação usar
    └── PreferenciasUsuario.kt            preferências salvas no aparelho (SharedPreferences)
```

Recursos (`app/src/main/res/`): `layout/` (telas e itens de lista), `drawable/` (fundos, ícones), `values/` (cores `gh_*`, textos em `strings.xml`, tema), `font/` (Exo 2).

---

## 5. Para o Backend/Database: como conectar os dados reais

### 5.1 O contrato: `RepositorioDeJogos`

```kotlin
interface RepositorioDeJogos {
    suspend fun fileiras(): List<Fileira>                              // tela Principal
    suspend fun buscarPorTitulo(texto: String): List<Jogo>             // busca (RF02)
    suspend fun jogosDaFileira(tituloDaFileira: String): List<Jogo>    // botão "Ver tudo"
}
```

- As funções são **`suspend`**: as telas já as chamam dentro de corrotinas (`lifecycleScope.launch`), então vocês podem fazer chamadas de rede ou de banco à vontade, **sem travar a tela** (RNF04).
- Se der erro (sem internet, HTTP 429/5xx, timeout), **lancem uma exceção**. O tratamento visual (loading, erro, "sem internet", RF10) será feito pelo Frontend em cima disso.
- Se precisarem de **novas funções** (ex.: favoritos, histórico de preços, paginação), adicionem-nas na interface e avisem o Frontend para criar a parte visual.

### 5.2 Passo a passo para trocar os dados de exemplo pelos reais

1. Criem uma classe nova em `dados/` (ou num pacote de vocês) que implemente a interface:
   ```kotlin
   class RepositorioDeJogosApi : RepositorioDeJogos {
       override suspend fun fileiras(): List<Fileira> { /* CheapShark /deals + RAWG... */ }
       override suspend fun buscarPorTitulo(texto: String): List<Jogo> { /* CheapShark /games... */ }
       override suspend fun jogosDaFileira(tituloDaFileira: String): List<Jogo> { /* ... */ }
   }
   ```
2. Em `dados/Repositorios.kt`, troquem **uma única linha**:
   ```kotlin
   object Repositorios {
       val jogos: RepositorioDeJogos = RepositorioDeJogosApi()   // antes: RepositorioDeJogosDeExemplo
   }
   ```
3. Pronto: Principal, Resultado de pesquisas, "Ver tudo" e os modais passam a mostrar os dados reais.
4. Quando não for mais necessário, `RepositorioDeJogosDeExemplo.kt` pode ser apagado.

> **Importante:** convertam as respostas das APIs para os modelos da [seção 6](#6-formato-dos-dados-modelos) (`Jogo`, `Oferta`, `Fileira`...). Se algum campo não fizer sentido com o que a API devolve, conversem com o Frontend antes de mudar o modelo, porque as telas leem esses campos.

### 5.3 Conta do usuário (`PreferenciasUsuario`)

`dados/PreferenciasUsuario.kt` guarda dados no aparelho com **SharedPreferences**. Ele tem dois tipos de dado:

| Dado | Situação |
|---|---|
| Moeda padrão, alertas de promoção, moedas escolhidas no "Comparar moedas" | Preferências de tela, **podem continuar no aparelho** |
| Nome, e-mail, data de nascimento | **PROVISÓRIO**: só existe para o menu de perfil e as Configurações funcionarem sem servidor |

Quando o Backend de contas existir:
- **Login** (`MainActivity`), **Cadastro** (`CadastroActivity`), **Recuperar senha** (`RecuperarSenhaActivity`) e **Configurações** (`ConfiguracoesActivity`) devem chamar o serviço de vocês. Os pontos exatos estão marcados com `TODO (Backend)` no código.
- As funções `salvarEmail`/`salvarPerfil` e as leituras de `nome`, `email` e `dataNascimento` saem do `PreferenciasUsuario`.
- O **Sair** (`PrincipalActivity`) deve encerrar a sessão (ex.: apagar o token salvo).

### 5.4 Regras que hoje estão no app e podem ir para o Backend

Para as telas funcionarem com dados de exemplo, alguns cálculos ficaram no próprio app. Se a API de vocês já entregar esses valores prontos, basta preenchê-los e simplificar os modelos:

| Onde | O que faz hoje | Alternativa com a API |
|---|---|---|
| `Jogo.precoMedioHoje`, `Jogo.desconto`, `Jogo.emPromocao` | calculados a partir da lista de ofertas | a CheapShark já devolve `salePrice`, `normalPrice` e `savings` |
| `Moeda` | cotações **fixas** de exemplo (ex.: US$ 1 = R$ 5,50) | API de câmbio |
| `Filtros.aplicar()` | filtra e ordena a lista **no aparelho** | a CheapShark aceita filtros e ordenação na própria requisição |
| `Loja.linkDeBusca()` | abre a **busca** do jogo no site da loja | link de redirecionamento da oferta (`dealID`) da CheapShark |
| `Jogo.capaUrl` | sempre `null` (as capas são um degradê) | URL da capa vinda da API; o Frontend carrega a imagem (Glide/Coil) |

---

## 6. Formato dos dados (modelos)

Todos ficam em `modelo/`. **Preços em reais (R$)**, exceto `precoEmDolar`.

```kotlin
data class Jogo(
    val id: Int,
    val titulo: String,
    val capaUrl: String?,            // null enquanto não houver imagem
    val nota: Double,                // 0 a 10
    val precoLancamento: Double,     // preço normal, em R$
    val plataformas: List<Plataforma>,
    val genero: String,
    val desenvolvedora: String,
    val anoLancamento: Int,
    val resumo: String,
    val precoEmDolar: Double,        // preço de tabela nos EUA (base da comparação de moedas)
    val ofertas: List<Oferta>        // preço atual em cada loja
)

data class Oferta(val loja: Loja, val preco: Double)                          // preço atual em R$
data class Fileira(val titulo: String, val subtitulo: String, val jogos: List<Jogo>)
```

| Enum | Valores |
|---|---|
| `Plataforma` | PC, PS5, PS4, PS3, PS2, XBOX_SERIES, XBOX_ONE, XBOX_360, SWITCH, SWITCH_2 |
| `Loja` | STEAM, EPIC_GAMES, NUUVEM, PLAYSTATION_STORE, MICROSOFT_STORE, NINTENDO_ESHOP |
| `Moeda` | USD, BRL, EUR, GBP, JPY, ARS, MXN, CAD |
| `Ordenacao` | opções de ordenação do modal de filtros |

> A CheapShark identifica lojas por `storeID` (RF13). Será preciso converter esse ID para o enum `Loja`, ou transformar `Loja` numa classe com nome e logo vindos de `/stores`, combinando com o Frontend.

---

## 7. Lista de pendências (TODOs) por área

Todas estão marcadas no código. Busquem por `TODO` no Android Studio (**View → Tool Windows → TODO**).

### Backend / Database
| Arquivo | O que falta |
|---|---|
| `dados/Repositorios.kt` | trocar `RepositorioDeJogosDeExemplo` pela implementação real |
| `dados/RepositorioDeJogosDeExemplo.kt` | criar a implementação real (CheapShark, RAWG/IGDB) |
| `dados/PreferenciasUsuario.kt` | perfil do usuário passar a vir do servidor |
| `MainActivity.kt` | autenticar e-mail e senha |
| `CadastroActivity.kt` | enviar nome, e-mail, senha, data de nascimento e foto |
| `RecuperarSenhaActivity.kt` | enviar o e-mail de recuperação |
| `ConfiguracoesActivity.kt` | enviar alterações do perfil, nova senha e foto |
| `PrincipalActivity.kt` | encerrar a sessão ao sair |
| `modelo/Moeda.kt` | buscar cotações na API de câmbio |
| `modelo/Loja.kt` | usar o link de redirecionamento da oferta (`dealID`) |
| Favoritos (RF07/RF08), amizades (RF16), histórico de preços (RF15), paginação (RF06) | ainda não existem; precisam de funções novas no repositório |

### Frontend (depois que os dados reais existirem)
| Arquivo | O que falta |
|---|---|
| `InicioFragment.kt`, `ResultadoPesquisaActivity.kt` | estados de loading, erro, vazio e "sem internet" (RF10) |
| `adaptadores/JogoAdapter.kt`, `modais/ModalJogo.kt`, `modal_comparar_moedas.xml` | carregar a imagem da capa (`capaUrl`) |
| `drawable/bg_selo_loja.xml` | usar o logo da loja vindo da API |
| Telas novas | favoritos, amigos/perfil de amigo, gráfico de histórico de preços, atualizar manualmente (RF11) |
| `ConfiguracoesActivity.kt` | usar a opção "alertas de promoção" para notificações |

---

## 8. Requisitos do professor: situação atual

Baseado no PDF *Requisitos Mínimos para o Projeto Final*:

| # | Requisito | Situação |
|---|---|---|
| 1 | Motivação e problema real | ✅ Descrito no documento de requisitos da equipe |
| 2 | Android + Kotlin | ✅ |
| 3 | Múltiplas Activities + pelo menos um Fragment | ✅ 6 Activities + `InicioFragment` |
| 4 | Persistência de dados (SQLite/Room ou Firebase/API REST) | ⏳ **Database**: hoje só há SharedPreferences para preferências de tela |
| 5 | Integração com serviço web | ⏳ **Backend**: CheapShark, RAWG/IGDB, câmbio |
| 6 | Repositório Git com acesso ao professor | ✅ Este repositório (lembrar de dar acesso ao professor) |
| 7 | Equipe de até 3 pessoas | ✅ |
| 8 | Apresentação em pitch | ⏳ |

Diferença a alinhar: o documento da equipe pede **API 26+** (RNF01), mas o app está com `minSdk = 24` em `app/build.gradle.kts`.

---

## 9. Convenções do código

- **Nomes em português** para classes, funções e variáveis do projeto (`buscarPorTitulo`, `jogosEncontrados`...).
- **Comentários por bloco de código**, explicando o que o trecho faz, não linha a linha.
- **viewBinding** em todas as telas (nada de `findViewById`).
- **Textos em `res/values/strings.xml`** e **cores em `res/values/colors.xml`** (prefixo `gh_`); nada de texto ou cor "fixo" no código.
- Pendências marcadas como `TODO (Área): descrição`, com área sendo `Backend`, `Database`, `API` ou `Frontend`.

---

## 10. O que não vai para o GitHub (.gitignore)

O `.gitignore` da raiz impede que subam:
- **`local.properties`**: caminho do SDK na máquina de cada um;
- pastas geradas: `build/`, `.gradle/`, `.kotlin/`, APKs (`*.apk`, `*.aab`);
- configurações pessoais do Android Studio: `.idea/`, `*.iml`;
- **chaves e segredos**: `*.jks`, `*.keystore`, `google-services.json`, `secrets.properties`, `apikeys.properties`, `.env`.

> ⚠️ **Nunca coloquem chaves de API diretamente no código.** Guardem-nas em `secrets.properties` (ou `apikeys.properties`) na raiz do projeto, que fica só na máquina de vocês, e leiam no `build.gradle.kts`. Compartilhem as chaves com a equipe por fora do GitHub.
