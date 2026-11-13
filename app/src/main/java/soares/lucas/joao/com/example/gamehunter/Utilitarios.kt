package soares.lucas.joao.com.example.gamehunter

import android.app.DatePickerDialog
import android.content.Context
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.Calendar
import java.util.Locale
import soares.lucas.joao.com.example.gamehunter.modelo.Moeda
import soares.lucas.joao.com.example.gamehunter.modelo.Plataforma

// Funções e constantes usadas por mais de uma tela. Por estarem fora de uma classe
// ("top-level"), podem ser chamadas de qualquer lugar do app.

const val TAMANHO_MINIMO_SENHA = 8
private const val CARACTERES_ESPECIAIS_SENHA = "!@#\$%&*"

// Quantidade máxima de etiquetas de plataforma nos cards
private const val MAXIMO_ETIQUETAS = 3

// Padrão brasileiro de números: vírgula nos decimais e ponto nos milhares (ex.: 1.249,90)
private val LOCALE_BRASIL: Locale = Locale.forLanguageTag("pt-BR")

// ===== VALIDAÇÃO DE CAMPOS =====
// Cada função mostra o erro no próprio campo (propriedade "error") e
// retorna true quando o campo está correto

fun emailValido(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun validarCampoObrigatorio(campo: EditText): Boolean {
    if (campo.text.toString().trim().isEmpty()) {
        campo.error = campo.context.getString(R.string.erro_campo_obrigatorio)
        return false
    }
    return true
}

fun validarCampoEmail(campo: EditText): Boolean {
    if (!validarCampoObrigatorio(campo)) {
        return false
    }
    if (!emailValido(campo.text.toString().trim())) {
        campo.error = campo.context.getString(R.string.erro_email_invalido)
        return false
    }
    return true
}

// Exige 8 caracteres, maiúscula, minúscula, número, símbolo e confirmação igual.
// Em Configurações, deixar os dois campos vazios significa "não alterar a senha".
fun validarNovaSenha(campoSenha: EditText, campoConfirmacao: EditText, obrigatoria: Boolean): Boolean {
    val contexto = campoSenha.context
    val senha = campoSenha.text.toString()
    val confirmacao = campoConfirmacao.text.toString()

    if (!obrigatoria && senha.isEmpty() && confirmacao.isEmpty()) {
        return true
    }

    var valido = true

    if (senha.isEmpty()) {
        campoSenha.error = contexto.getString(R.string.erro_campo_obrigatorio)
        valido = false
    } else if (senha.length < TAMANHO_MINIMO_SENHA) {
        campoSenha.error = contexto.getString(R.string.erro_senha_curta)
        valido = false
    } else if (senha.none { it in 'A'..'Z' }) {
        campoSenha.error = contexto.getString(R.string.erro_senha_maiuscula)
        valido = false
    } else if (senha.none { it in 'a'..'z' }) {
        campoSenha.error = contexto.getString(R.string.erro_senha_minuscula)
        valido = false
    } else if (senha.none { it in '0'..'9' }) {
        campoSenha.error = contexto.getString(R.string.erro_senha_numero)
        valido = false
    } else if (senha.none { it in CARACTERES_ESPECIAIS_SENHA }) {
        campoSenha.error = contexto.getString(R.string.erro_senha_especial)
        valido = false
    }

    if (confirmacao.isEmpty()) {
        campoConfirmacao.error = contexto.getString(R.string.erro_campo_obrigatorio)
        valido = false
    } else if (confirmacao != senha) {
        campoConfirmacao.error = contexto.getString(R.string.erro_senhas_diferentes)
        valido = false
    }

    return valido
}

// ===== CAMPOS DE SENHA, DATA E TECLADO =====

// Mostra a senha se estiver oculta (ou oculta se estiver visível), trocando também
// o ícone do olho e a descrição lida pelo leitor de tela. No fim, devolve o cursor
// para o final do texto, pois a troca o levaria para o início
fun alternarVisibilidadeSenha(campoSenha: EditText, iconeOlho: ImageView) {
    val contexto = campoSenha.context

    if (campoSenha.transformationMethod is PasswordTransformationMethod) {
        campoSenha.transformationMethod = HideReturnsTransformationMethod.getInstance()
        iconeOlho.setImageResource(R.drawable.ic_olho_ativo)
        iconeOlho.contentDescription = contexto.getString(R.string.ocultar_senha)
    } else {
        campoSenha.transformationMethod = PasswordTransformationMethod.getInstance()
        iconeOlho.setImageResource(R.drawable.ic_olho)
        iconeOlho.contentDescription = contexto.getString(R.string.mostrar_senha)
    }

    campoSenha.setSelection(campoSenha.text.length)
}

// Abre um calendário iniciando na data de hoje e escreve a data escolhida no campo,
// no formato DD/MM/AAAA. O mês vem de 0 a 11, por isso o "+ 1".
// maxDate impede escolher uma data no futuro
fun abrirCalendario(campoData: EditText) {
    val contexto = campoData.context
    val hoje = Calendar.getInstance()

    val dialogo = DatePickerDialog(
        contexto,
        { _, ano, mes, dia ->
            campoData.setText(contexto.getString(R.string.formato_data, dia, mes + 1, ano))
            campoData.error = null
        },
        hoje.get(Calendar.YEAR),
        hoje.get(Calendar.MONTH),
        hoje.get(Calendar.DAY_OF_MONTH)
    )

    dialogo.datePicker.maxDate = hoje.timeInMillis
    dialogo.show()
}

// Fecha o teclado virtual que está aberto para o campo informado
fun esconderTeclado(campo: View) {
    val teclado = campo.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    teclado.hideSoftInputFromWindow(campo.windowToken, 0)
}

// ===== FORMATAÇÃO DE NÚMEROS =====
// Preço com 2 casas (249.9 -> "249,90"), nota com 1 casa (9.6 -> "9,6") e valor
// com o símbolo da moeda (ex.: "US$ 69,99"). O "R$" dos preços em reais é
// adicionado pelo strings.xml (formato_preco)

fun formatarPreco(valor: Double): String = if (valor.isFinite()) String.format(LOCALE_BRASIL, "%,.2f", valor) else "—"

fun formatarNota(nota: Double): String = if (nota.isFinite()) String.format(LOCALE_BRASIL, "%.1f", nota) else "—"

fun formatarValorNaMoeda(contexto: Context, moeda: Moeda, valor: Double): String {
    if (!valor.isFinite()) return contexto.getString(R.string.itad_indisponivel)
    return contexto.getString(R.string.formato_moeda, moeda.simbolo, formatarPreco(valor))
}

// ===== COMPONENTES REUTILIZADOS =====

// Preenche as etiquetas de plataforma de um card (até 3): se o jogo tiver até 3
// plataformas, mostra todas; se tiver mais, mostra as 2 primeiras e um "+N" com
// o restante. As etiquetas que sobrarem ficam escondidas
fun preencherEtiquetasDePlataforma(etiquetas: List<TextView>, plataformas: List<Plataforma>) {
    val siglas = plataformas.map { it.sigla }

    val textos = if (siglas.size <= MAXIMO_ETIQUETAS) {
        siglas
    } else {
        val restantes = siglas.size - (MAXIMO_ETIQUETAS - 1)
        siglas.take(MAXIMO_ETIQUETAS - 1) +
            etiquetas.first().context.getString(R.string.card_mais_plataformas, restantes)
    }

    etiquetas.forEachIndexed { indice, etiqueta ->
        if (indice < textos.size) {
            etiqueta.text = textos[indice]
            etiqueta.visibility = View.VISIBLE
        } else {
            etiqueta.visibility = View.GONE
        }
    }
}

// Coloca uma View dentro de um Flow (ajudante do ConstraintLayout que organiza os
// itens lado a lado e "quebra a linha" quando não cabem mais). O Flow encontra as
// Views pelo id, por isso cada uma recebe um id novo antes de ser adicionada
fun adicionarAoFlow(flow: Flow, view: View) {
    val layoutPai = flow.parent as ConstraintLayout
    view.id = View.generateViewId()
    layoutPai.addView(
        view,
        ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    )
    flow.addView(view)
}

// Exibe um AlertDialog como uma "folha" presa na parte de baixo da tela, como os
// modais do Figma: fundo da janela transparente (aparece só o fundo arredondado do
// layout), alinhado embaixo e ocupando toda a largura. O escurecimento atrás e o
// fechamento ao tocar fora já são feitos pelo próprio AlertDialog
fun exibirComoFolhaInferior(dialogo: AlertDialog) {
    dialogo.show()
    dialogo.window?.let { janela ->
        janela.setBackgroundDrawableResource(android.R.color.transparent)
        janela.setGravity(Gravity.BOTTOM)
        janela.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}
