package soares.lucas.joao.com.example.gamehunter.modais

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.cancel
import soares.lucas.joao.com.example.gamehunter.componentes.CarregadorPrecosRegionais
import soares.lucas.joao.com.example.gamehunter.dados.Repositorios
import com.bumptech.glide.Glide
import soares.lucas.joao.com.example.gamehunter.formatarValorNaMoeda
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import soares.lucas.joao.com.example.gamehunter.R
import soares.lucas.joao.com.example.gamehunter.adaptadores.OfertaAdapter
import soares.lucas.joao.com.example.gamehunter.adaptadores.PrecoMoedaAdapter
import soares.lucas.joao.com.example.gamehunter.adicionarAoFlow
import soares.lucas.joao.com.example.gamehunter.dados.PreferenciasUsuario
import soares.lucas.joao.com.example.gamehunter.databinding.ItemTagPlataformaBinding
import soares.lucas.joao.com.example.gamehunter.databinding.ModalJogoBinding
import soares.lucas.joao.com.example.gamehunter.exibirComoFolhaInferior
import soares.lucas.joao.com.example.gamehunter.formatarNota
import soares.lucas.joao.com.example.gamehunter.modelo.Jogo
import soares.lucas.joao.com.example.gamehunter.modelo.Oferta

// Modal com os DETALHES DO JOGO (RF03 e RF04): informações, preços, preço em outras
// moedas e as lojas onde comprar. Tocar em "Ir" abre a loja no navegador (RF09)
class ModalJogo(
    private val contexto: Context,
    private val jogo: Jogo
) {

    // Quantidade de lojas exibidas antes de tocar em "Ver mais lojas" e quantidade
    // de colunas da grade de preços por moeda
    companion object {
        private const val LOJAS_INICIAIS = 3
        private const val COLUNAS_MOEDAS = 3
    }

    private val binding = ModalJogoBinding.inflate(LayoutInflater.from(contexto))
    private val preferencias = PreferenciasUsuario(contexto)

    private val ofertas = jogo.ofertasPorPreco
    private var mostrandoTodasAsLojas = false

    private val ofertaAdapter = OfertaAdapter(ofertas.take(LOJAS_INICIAIS)) { oferta -> abrirLoja(oferta) }
    private val precoMoedaAdapter: PrecoMoedaAdapter = PrecoMoedaAdapter(preferencias.moedasSelecionadas) { tentarNovamente(it) }
    private val escopo = contexto.criarEscopoDoModal()
    private val carregador = CarregadorPrecosRegionais(escopo, Repositorios.jogos(contexto), jogo.idItad) {
        precoMoedaAdapter.atualizarPrecos(it)
    }

    private fun tentarNovamente(moeda: Moeda) {
        carregador.carregar(listOf(moeda))
    }

    // Preenche cada parte do modal, cria o AlertDialog com o layout e o exibe
    // na parte de baixo da tela
    fun exibir() {
        preencherInformacoes()
        preencherPrecos()
        configurarMoedas()
        configurarLojas()

        val dialogo = AlertDialog.Builder(contexto)
            .setView(binding.root)
            .create()

        dialogo.setOnDismissListener { escopo.cancel() }
        binding.btnFechar.setOnClickListener { dialogo.dismiss() }
        exibirComoFolhaInferior(dialogo)
        carregador.carregar(preferencias.moedasSelecionadas)
    }

    // Capa, nome, "gênero · desenvolvedora · ano", nota, plataformas e resumo.
    // Enquanto não há imagem, a capa mostra o título sobre um degradê.
    // Cada plataforma vira uma etiqueta criada a partir do layout item_tag_plataforma
    // e colocada no Flow, ao lado da caixa de avaliação.
    private fun preencherInformacoes() {
        binding.frameCapa.clipToOutline = true
        Glide.with(binding.imgCapa).load(jogo.capaUrl).centerCrop().into(binding.imgCapa)
        binding.txtTituloCapa.text = jogo.titulo
        binding.imgCapa.contentDescription = contexto.getString(R.string.desc_capa_jogo, jogo.titulo)

        binding.txtNome.text = jogo.titulo
        binding.txtInformacoes.text = listOf(
            jogo.genero.ifBlank { contexto.getString(R.string.itad_indisponivel) },
            jogo.desenvolvedora.ifBlank { contexto.getString(R.string.itad_indisponivel) },
            jogo.anoLancamento.takeIf { it > 0 }?.toString() ?: contexto.getString(R.string.itad_indisponivel)
        ).joinToString(" · ")

        val nota = formatarNota(jogo.nota)
        binding.txtNota.text = nota
        binding.caixaAvaliacao.contentDescription = contexto.getString(R.string.desc_nota_detalhe, nota)

        val inflater = LayoutInflater.from(contexto)
        jogo.plataformas.forEach { plataforma ->
            val etiqueta = ItemTagPlataformaBinding.inflate(inflater).root
            etiqueta.text = plataforma.sigla
            adicionarAoFlow(binding.flowPlataformas, etiqueta)
        }

        binding.txtResumo.text = jogo.resumo.ifBlank { contexto.getString(R.string.itad_sem_sinopse) }
    }

    // Preço de lançamento, média de hoje e o selo de desconto (escondido quando o
    // jogo não está em promoção)
    private fun preencherPrecos() {
        binding.txtPrecoLancamento.text =
            formatarValorNaMoeda(contexto, Moeda.BRL, jogo.precoLancamento)
        binding.txtPrecoMedio.text =
            formatarValorNaMoeda(contexto, Moeda.BRL, jogo.precoMedioHoje)

        if (jogo.emPromocao) {
            binding.txtDesconto.text = contexto.getString(R.string.detalhe_desconto, jogo.desconto)
            binding.txtDesconto.visibility = View.VISIBLE
        } else {
            binding.txtDesconto.visibility = View.GONE
        }
    }

    // Grade (3 colunas) com o preço nas moedas escolhidas pelo usuário. "Escolher moedas"
    // abre o modal Comparar moedas; depois de aplicar, a grade é atualizada com as
    // novas moedas salvas nas preferências
    private fun configurarMoedas() {
        binding.rvPrecosMoedas.layoutManager = GridLayoutManager(contexto, COLUNAS_MOEDAS)
        binding.rvPrecosMoedas.adapter = precoMoedaAdapter

        binding.btnEscolherMoedas.setOnClickListener {
            ModalCompararMoedas(contexto, jogo) {
                precoMoedaAdapter.atualizarMoedas(preferencias.moedasSelecionadas)
                carregador.carregar(preferencias.moedasSelecionadas)
            }.exibir()
        }
    }

    // Lista de lojas, da mais barata para a mais cara. Mostra só as primeiras e o
    // botão "Ver mais lojas" alterna entre a lista curta e a completa (o botão
    // nem aparece quando o jogo tem poucas lojas)
    private fun configurarLojas() {
        binding.rvOfertas.layoutManager = LinearLayoutManager(contexto)
        binding.rvOfertas.adapter = ofertaAdapter

        binding.btnVerMaisLojas.visibility =
            if (ofertas.size > LOJAS_INICIAIS) View.VISIBLE else View.GONE

        binding.btnVerMaisLojas.setOnClickListener {
            mostrandoTodasAsLojas = !mostrandoTodasAsLojas

            if (mostrandoTodasAsLojas) {
                ofertaAdapter.atualizarOfertas(ofertas)
                binding.btnVerMaisLojas.setText(R.string.detalhe_ver_menos_lojas)
            } else {
                ofertaAdapter.atualizarOfertas(ofertas.take(LOJAS_INICIAIS))
                binding.btnVerMaisLojas.setText(R.string.detalhe_ver_mais_lojas)
            }
        }
    }

    // Abre a página da loja no navegador com uma Intent implícita (ACTION_VIEW).
    // Antes, confere se existe algum app capaz de abrir o link; se não houver,
    // avisa o usuário com um Toast
    private fun abrirLoja(oferta: Oferta) {
        val url = oferta.url ?: oferta.loja?.linkDeBusca(jogo.titulo) ?: return
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())

        if (intent.resolveActivity(contexto.packageManager) != null) {
            contexto.startActivity(intent)
        } else {
            Toast.makeText(contexto, R.string.erro_abrir_link, Toast.LENGTH_SHORT).show()
        }
    }
}
