# Conexão com IsThereAnyDeal

## Configurar e executar

1. Cadastre o aplicativo em https://isthereanydeal.com/apps/my/ e obtenha a API key (não o OAuth client secret).
2. Na raiz, copie `secrets.properties.example` para `secrets.properties`, se ainda não existir.
3. Preencha localmente: `ITAD_API_KEY=sua_chave`. Não coloque aspas e não envie a chave no chat ou em commits.
4. Sincronize o Gradle e execute novamente o aplicativo. Mudar a chave exige recompilar.
5. O login continua demonstrativo: e-mail em formato válido e senha não vazia. Depois de entrar, aguarde as ofertas.
6. Pesquise um título, abra os detalhes e toque em **Ir** para conferir o link da loja. **Ver tudo** reutiliza a seleção inicial.

A variável de ambiente `ITAD_API_KEY` tem prioridade sobre o arquivo.
Sem chave, a compilação funciona, mas o catálogo informa que a conexão não foi configurada.

`secrets.properties`, `local.properties`, `.gradle/`, `build/` e APKs são ignorados pelo Git.
Confira antes de publicar: `git check-ignore secrets.properties`.
A chave é incluída no `BuildConfig` e pode ser extraída do APK. Esta solução protege o
repositório, não o aplicativo distribuído. A migração para um backend fica para outra etapa.

## O que está conectado

- `GET /deals/v2`: seleção inicial de até 20 ofertas para o Brasil.
- `GET /games/search/v1`: até 20 resultados por pesquisa.
- `GET /games/info/v2`: capa, tags, desenvolvedora, lançamento e avaliações disponíveis.
- `POST /games/prices/v3`: preços por loja para `country=BR` no catálogo; nos modais, consulta por país das moedas selecionadas, sem cupons.
- Capas carregadas por Glide nos espaços existentes e links de compra preservados, incluindo afiliados.
- HTTPS em segundo plano, timeout, cache em memória de cinco minutos e até três consultas simultâneas de detalhes.
- Mensagens nos componentes existentes para carregamento, chave ausente/inválida, falha de rede e limite de consultas.

Para tentar novamente na inicial, reabra a tela; na pesquisa, envie a consulta novamente.
A aplicação usa somente IsThereAnyDeal como API de catálogo/preços. Não há alterações
de autenticação nem serviço backend novo. Layouts, cores, fontes e navegação foram
preservados; os textos das fileiras descrevem ofertas reais, sem alegar um ranking de
vendas ou personalização que a integração não implementa.

## Limites desta etapa

- Ofertas para PC. Os chips e o modal filtram por Ação, Aventura, RPG, Estratégia, Simulação, Corrida, Esportes e Terror.
- Na inicial e em “Ver tudo”, gêneros são enviados à API por `tagsUnion` (qualquer gênero selecionado). Na pesquisa por título, filtram as tags completas dos até 20 resultados retornados. “Todos” remove a seleção de gênero.
- No catálogo, só ofertas explicitamente em BRL entram na média. Nos modais, a moeda retornada precisa coincidir com a moeda escolhida. USD não é apresentado como reais ou pesos.
- Lojas fora do enum aparecem com seu nome real. Os filtros mantêm as opções atuais.
- Preço regular não é preço de lançamento. Este último fica como `—`. Desconto é calculado sobre preços regulares das mesmas ofertas.
- Nota usa Metascore ou avaliação Steam, quando disponível, na escala de 0 a 10.
- Sinopse não vem da API e recebe mensagem de indisponibilidade; tags ocupam o espaço de gênero.
- Jogos sem ofertas em BRL continuam pesquisáveis, com preço `—`, nunca como gratuitos.
- Preços internacionais são consultados sob demanda nos modais. Conversão cambial e cálculo de economia entre países continuam indisponíveis; não há API de câmbio nem cotações fictícias.
- Cache em memória; sem persistência offline, paginação ou histórico. A segunda fileira ordena a mesma seleção por desconto; não é um ranking global.
- Falhas nunca fazem o aplicativo trocar silenciosamente para dados de exemplo.

## Preços internacionais

O endpoint público recebe `country`, não um parâmetro livre de moeda. Cada oferta
informa sua moeda. As opções do aplicativo consultam os seguintes países:

| Opção | País consultado | Código |
|---|---|---|
| BRL | Brasil | BR |
| USD | Estados Unidos | US |
| EUR | Alemanha (referência escolhida para o euro) | DE |
| GBP | Reino Unido | GB |
| JPY | Japão | JP |
| ARS | Argentina | AR |
| MXN | México | MX |
| CAD | Canadá | CA |

Esta tabela descreve a configuração do aplicativo, não uma garantia de cobertura de
todas as lojas/jogos em todas essas moedas. Uma consulta pode não retornar ofertas na
moeda escolhida. Por exemplo, se uma loja devolver USD para Argentina, esse preço não
será exibido como ARS. Nesse caso a linha informa **Sem ofertas**.

Ao abrir os detalhes ou o modal de moedas, carregam-se somente as moedas selecionadas.
Ao selecionar outras, elas são consultadas sem recarregar o catálogo. Cada preço é a
média das ofertas atuais naquela moeda/país, sem cupons. O cache compartilhado entre
modais dura cinco minutos, limitado a 128 pares jogo/moeda e três consultas regionais
simultâneas. Fechar o diálogo ou destruir a Activity cancela suas consultas.

Falhas de rede/autorização aparecem como **Indisponível**, preservando os valores das
outras regiões. Toque na linha/caixa que falhou para tentar novamente. Ausência de
ofertas é diferente de falha, e um preço zero retornado pela API continua sendo zero.

Os valores são preços regionais originais, não conversões do preço brasileiro.
O visual foi preservado; os textos agora indicam média atual e ausência de conversão.
As cotações e fatores regionais fixos do protótipo foram removidos. Para comparar
numericamente a economia entre países será necessária uma fonte pública de câmbio;
o endpoint de taxas `/internal/rates/v1` da IsThereAnyDeal é documentado como interno
e não é usado pelo aplicativo.

## Validação

Execute `gradlew.bat :app:assembleDebug :app:testDebugUnitTest`.
`IntegracaoItadTest` usa respostas locais para verificar mapeamento, moeda, preço zero
versus ausente, lojas desconhecidas, URL, cache, expiração e erros. Não exige chave
nem faz chamadas autenticadas.
`PrecosRegionaisItadTest` verifica médias, separação de moedas, países, cache,
ausência de ofertas, falhas parciais, nova tentativa e cancelamento ao fechar o modal.

Para validar ao vivo, configure sua chave e teste inicial, pesquisa, filtros, detalhes e
links. Confira também o comportamento sem internet e sem chave. Não registre chaves
ou cabeçalhos de autenticação em logs.

Documentação: https://docs.isthereanydeal.com/ (schema: https://github.com/IsThereAnyDeal/API).
Confira os termos antes de distribuir o app: incluem restrições relativas a aplicações
concorrentes e preservação dos dados/links fornecidos.

Na validação desta etapa, as chamadas ao vivo feitas pelo ambiente de desenvolvimento
receberam HTTP 403 / Cloudflare 1010 (assinatura do cliente bloqueada). Isso não
confirma falha da chave. A disponibilidade por moeda precisa ser conferida no Android.
