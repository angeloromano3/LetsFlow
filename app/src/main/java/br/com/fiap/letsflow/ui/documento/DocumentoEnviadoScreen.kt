package br.com.fiap.letsflow.ui.documento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// Tela de sucesso depois do envio: tudo é só leitura, nada pode ser alterado aqui
@Composable
fun DocumentoEnviadoScreen(
    documento: ResumoDocumentoUi,
    participantes: List<ParticipanteResumoUi>,
    mensagemEnviada: String,
    onVerDetalhes: () -> Unit,
    onVoltar: () -> Unit,
    onVisualizarPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val textoDeSignatarios = if (participantes.size == 1) {
        "1 signatário vinculado."
    } else {
        "${participantes.size} signatários vinculados."
    }

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
                titulo = "Documento enviado",
                subtitulo = "Envio registrado · configurações preservadas",
                onVoltar = onVoltar
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cores.destaque)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Documento enviado",
                    style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.acao
                )
                Text(
                    text = "$textoDeSignatarios O acompanhamento já está disponível.",
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CartaoResumoDoDocumento(
                documento = documento,
                onVisualizarPdf = onVisualizarPdf
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sem os links de editar: o envio está bloqueado
            CartaoResumoDeParticipantes(participantes = participantes)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mensagem enviada",
                style = tipografia.labelLarge,
                color = cores.texto
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mensagemEnviada,
                style = tipografia.bodyMedium,
                color = cores.texto,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cores.superficie)
                    .border(1.dp, cores.divisao, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Este envio está bloqueado para edição. Para alterar participantes " +
                        "ou campos, será necessário iniciar um novo fluxo.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        BotaoPrimario(
            texto = "Ver detalhes do documento",
            onClick = onVerDetalhes,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

@Preview(name = "Documento enviado", showBackground = true, showSystemUi = true)
@Composable
private fun DocumentoEnviadoPreview() {
    LetsFlowTheme {
        DocumentoEnviadoScreen(
            documento = documentoDeExemploUi(),
            participantes = participantesDeExemploUi(),
            mensagemEnviada = "Você recebeu um documento para assinatura.",
            onVerDetalhes = {},
            onVoltar = {},
            onVisualizarPdf = {}
        )
    }
}