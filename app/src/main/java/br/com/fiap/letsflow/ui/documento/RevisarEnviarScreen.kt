package br.com.fiap.letsflow.ui.documento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// Resumo do documento mostrado nas telas de revisão e de envio concluído
data class ResumoDocumentoUi(
    val titulo: String,
    val totalPaginas: Int,
    val nomeArquivo: String
)

// Uma pessoa do documento, com o papel e onde ficam os campos dela
data class ParticipanteResumoUi(
    val nome: String,
    val papel: String,
    val quantidadeDeCampos: Int,
    val paginas: String // Já formatado, por exemplo "Págs. 3–4"
)

// Etapa 4 do fluxo: o Admin confere tudo e confirma o envio
@Composable
fun RevisarEnviarScreen(
    documento: ResumoDocumentoUi,
    participantes: List<ParticipanteResumoUi>,
    mensagemInicial: String,
    onConfirmarEnvio: (mensagemDeConvite: String) -> Unit,
    onVoltar: () -> Unit,
    onVisualizarPdf: () -> Unit,
    onEditarParticipantes: () -> Unit,
    onAjustarCampos: () -> Unit,
    modifier: Modifier = Modifier,
    enviando: Boolean = false,
    envioFalhou: Boolean = false
) {
    var mensagemDeConvite by rememberSaveable { mutableStateOf(mensagemInicial) }

    RevisarEnviarConteudo(
        documento = documento,
        participantes = participantes,
        mensagemDeConvite = mensagemDeConvite,
        enviando = enviando,
        envioFalhou = envioFalhou,
        aoMudarMensagem = { mensagemDeConvite = it },
        onConfirmarEnvio = { onConfirmarEnvio(mensagemDeConvite.trim()) },
        onVoltar = onVoltar,
        onVisualizarPdf = onVisualizarPdf,
        onEditarParticipantes = onEditarParticipantes,
        onAjustarCampos = onAjustarCampos,
        modifier = modifier
    )
}

// Separado da tela para o Preview mostrar cada situação do Figma
@Composable
private fun RevisarEnviarConteudo(
    documento: ResumoDocumentoUi,
    participantes: List<ParticipanteResumoUi>,
    mensagemDeConvite: String,
    enviando: Boolean,
    envioFalhou: Boolean,
    aoMudarMensagem: (String) -> Unit,
    onConfirmarEnvio: () -> Unit,
    onVoltar: () -> Unit,
    onVisualizarPdf: () -> Unit,
    onEditarParticipantes: () -> Unit,
    onAjustarCampos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val podeConfirmar = mensagemDeConvite.isNotBlank() && !enviando

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            CabecalhoDaTelaDeResumo(
                titulo = "Revisar e enviar",
                subtitulo = "Etapa 4 de 4 · Confira antes de confirmar",
                onVoltar = onVoltar
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (envioFalhou) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cores.fundoErro)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "O envio não foi concluído",
                        style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = cores.erro
                    )
                    Text(
                        text = "Nenhum novo fluxo foi registrado. Você pode tentar novamente.",
                        style = tipografia.bodySmall,
                        color = cores.textoSecundario
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            CartaoResumoDoDocumento(
                documento = documento,
                onVisualizarPdf = onVisualizarPdf
            )

            Spacer(modifier = Modifier.height(12.dp))

            CartaoResumoDeParticipantes(
                participantes = participantes,
                onEditarParticipantes = onEditarParticipantes,
                onAjustarCampos = onAjustarCampos
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mensagem de convite *",
                style = tipografia.labelLarge,
                color = cores.texto
            )

            Spacer(modifier = Modifier.height(8.dp))

            CampoDaMensagemDeConvite(
                valor = mensagemDeConvite,
                aoMudarValor = aoMudarMensagem
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Após o envio, o documento e as posições dos campos serão preservados.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Botão fixo no rodapé, fora da rolagem
        BotaoPrimario(
            texto = if (enviando) "Enviando..." else "Confirmar envio →",
            onClick = onConfirmarEnvio,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            habilitado = podeConfirmar
        )
    }
}

// Seta, título e subtítulo no mesmo padrão das telas do fluxo de documento
@Composable
internal fun CabecalhoDaTelaDeResumo(
    titulo: String,
    subtitulo: String,
    onVoltar: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "←",
            fontSize = 22.sp,
            color = cores.texto,
            modifier = Modifier
                .semantics { contentDescription = "Voltar" }
                .clip(CircleShape)
                .clickable(role = Role.Button) { onVoltar() }
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = titulo,
            style = tipografia.headlineMedium,
            color = cores.texto
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = subtitulo,
        style = tipografia.bodySmall,
        color = cores.textoSecundario
    )
}

@Composable
internal fun CartaoResumoDoDocumento(
    documento: ResumoDocumentoUi,
    onVisualizarPdf: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val textoDePaginas = if (documento.totalPaginas == 1) {
        "1 página"
    } else {
        "${documento.totalPaginas} páginas"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cores.superficie)
            .border(1.dp, cores.divisao, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Documento",
            style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = cores.texto
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "▤", style = tipografia.bodySmall, color = cores.textoSecundario)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = documento.titulo,
                style = tipografia.bodyMedium,
                color = cores.texto
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$textoDePaginas · ${documento.nomeArquivo}",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        Text(
            text = "Visualizar PDF",
            style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = cores.acao,
            modifier = Modifier
                .clickable(role = Role.Button) { onVisualizarPdf() }
                .padding(top = 8.dp)
        )
    }
}

// Os dois links do rodapé só aparecem quando os dois toques são passados (tela de revisão)
@Composable
internal fun CartaoResumoDeParticipantes(
    participantes: List<ParticipanteResumoUi>,
    onEditarParticipantes: (() -> Unit)? = null,
    onAjustarCampos: (() -> Unit)? = null
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val textoDeQuantidade = if (participantes.size == 1) {
        "1 pessoa"
    } else {
        "${participantes.size} pessoas"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cores.superficie)
            .border(1.dp, cores.divisao, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Participantes e campos",
                style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = cores.texto,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = textoDeQuantidade,
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )
        }

        participantes.forEach { participante ->
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = participante.nome,
                        style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = cores.texto
                    )
                    Text(
                        text = "${participante.papel} · " +
                                textoDeCampos(participante.quantidadeDeCampos),
                        style = tipografia.bodySmall,
                        color = cores.textoSecundario
                    )
                }
                Text(
                    text = participante.paginas,
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinhaDivisoria()
        }

        if (onEditarParticipantes != null && onAjustarCampos != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Editar participantes",
                    style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.acao,
                    modifier = Modifier.clickable(role = Role.Button) { onEditarParticipantes() }
                )
                Text(
                    text = "Ajustar campos",
                    style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.acao,
                    modifier = Modifier.clickable(role = Role.Button) { onAjustarCampos() }
                )
            }
        }
    }
}

@Composable
private fun LinhaDivisoria() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LetsFlowTheme.cores.divisao)
    )
}

@Composable
private fun CampoDaMensagemDeConvite(
    valor: String,
    aoMudarValor: (String) -> Unit
) {
    val cores = LetsFlowTheme.cores
    val formaDoCampo = RoundedCornerShape(12.dp)

    BasicTextField(
        value = valor,
        onValueChange = aoMudarValor,
        maxLines = 4,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = cores.texto),
        cursorBrush = SolidColor(cores.acao),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(cores.superficie, formaDoCampo)
            .border(1.dp, cores.divisao, formaDoCampo)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

private fun textoDeCampos(quantidade: Int): String =
    if (quantidade == 1) "1 campo" else "$quantidade campos"

internal fun documentoDeExemploUi() = ResumoDocumentoUi(
    titulo = "Contrato de Prestação de Serviços",
    totalPaginas = 4,
    nomeArquivo = "contrato_servicos.pdf"
)

internal fun participantesDeExemploUi() = listOf(
    ParticipanteResumoUi(
        nome = "Mariana Oliveira",
        papel = "Contratante",
        quantidadeDeCampos = 2,
        paginas = "Págs. 3–4"
    ),
    ParticipanteResumoUi(
        nome = "Carlos Almeida",
        papel = "Contratado",
        quantidadeDeCampos = 1,
        paginas = "Pág. 4"
    )
)

@Preview(name = "Revisar", showBackground = true, showSystemUi = true)
@Composable
private fun RevisarEnviarPreview() {
    LetsFlowTheme {
        RevisarEnviarConteudo(
            documento = documentoDeExemploUi(),
            participantes = participantesDeExemploUi(),
            mensagemDeConvite = "Você recebeu um documento para assinatura.",
            enviando = false,
            envioFalhou = false,
            aoMudarMensagem = {},
            onConfirmarEnvio = {},
            onVoltar = {},
            onVisualizarPdf = {},
            onEditarParticipantes = {},
            onAjustarCampos = {}
        )
    }
}

@Preview(name = "Mensagem editada", showBackground = true, showSystemUi = true)
@Composable
private fun RevisarEnviarMensagemEditadaPreview() {
    LetsFlowTheme {
        RevisarEnviarConteudo(
            documento = documentoDeExemploUi(),
            participantes = participantesDeExemploUi(),
            mensagemDeConvite = "Olá! Por favor, revise e assine este contrato.",
            enviando = false,
            envioFalhou = false,
            aoMudarMensagem = {},
            onConfirmarEnvio = {},
            onVoltar = {},
            onVisualizarPdf = {},
            onEditarParticipantes = {},
            onAjustarCampos = {}
        )
    }
}

@Preview(name = "Envio não concluído", showBackground = true, showSystemUi = true)
@Composable
private fun RevisarEnviarFalhaPreview() {
    LetsFlowTheme {
        RevisarEnviarConteudo(
            documento = documentoDeExemploUi(),
            participantes = participantesDeExemploUi(),
            mensagemDeConvite = "Você recebeu um documento para assinatura.",
            enviando = false,
            envioFalhou = true,
            aoMudarMensagem = {},
            onConfirmarEnvio = {},
            onVoltar = {},
            onVisualizarPdf = {},
            onEditarParticipantes = {},
            onAjustarCampos = {}
        )
    }
}