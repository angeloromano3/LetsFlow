package br.com.fiap.letsflow.ui.documento

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.ui.componentes.ControlesZoom
import br.com.fiap.letsflow.ui.componentes.ZOOM_MINIMO
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// Um campo de assinatura desenhado em cima da página. As posições são frações da página (0 a 1)
data class CampoVisualizacaoUi(
    val rotulo: String,
    val posicaoX: Float,
    val posicaoY: Float,
    val largura: Float,
    val altura: Float
)

// Modo somente leitura: mostra a página do PDF e os campos, sem permitir mexer em nada
@Composable
fun VisualizarDocumentoAdminScreen(
    tituloDoDocumento: String,
    paginaAtual: Bitmap?, // Nulo enquanto a página ainda está sendo desenhada
    indicePagina: Int, // Começa em 0
    totalPaginas: Int,
    camposDaPagina: List<CampoVisualizacaoUi>,
    onPaginaAnterior: () -> Unit,
    onProximaPagina: () -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    // Zoom atual da página: 1 = largura da área, até 3 vezes maior (mesmo padrão da tela do signatário)
    var zoom by remember { mutableFloatStateOf(ZOOM_MINIMO) }

    val proporcaoDaPagina = if (paginaAtual != null) {
        paginaAtual.width / paginaAtual.height.toFloat()
    } else {
        PROPORCAO_DE_FOLHA_A4
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
            // Sem rolagem na tela toda: agora só a área da página rola, para o zoom funcionar
            .padding(horizontal = 16.dp) // Margem menor deixa a página maior
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        CabecalhoDaTelaDeResumo(
            titulo = "Visualizar documento",
            subtitulo = tituloDoDocumento,
            onVoltar = onVoltar
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Modo somente leitura",
            style = tipografia.titleLarge,
            color = cores.acao
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Este documento não pode ser alterado nesta visualização.",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Documento disponível para consulta.",
            style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Moldura em volta da página, como no Figma
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f) // Ocupa todo o espaço que sobra entre os textos e a navegação
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cores.destaque)
        ) {
            // Largura da página sem zoom: a área da moldura menos as margens
            val larguraBase = maxWidth - 12.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Com zoom, dá para arrastar para cima e para baixo
                    .horizontalScroll(rememberScrollState()) // E também para os lados
                    .padding(6.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .width(larguraBase * zoom) // A página cresce de verdade com o zoom
                        .aspectRatio(proporcaoDaPagina)
                        // O PDF é sempre branco, como papel, mesmo no modo escuro
                        .background(Color.White)
                ) {
                    if (paginaAtual != null) {
                        Image(
                            bitmap = paginaAtual.asImageBitmap(),
                            contentDescription = "Página ${indicePagina + 1} do documento",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    camposDaPagina.forEach { campo ->
                        CampoSomenteLeitura(
                            campo = campo,
                            larguraDaPagina = maxWidth,
                            alturaDaPagina = maxHeight
                        )
                    }
                }
            }

            // Mesmos botões de zoom da tela do signatário, flutuando no canto inferior direito
            ControlesZoom(
                zoom = zoom,
                onZoomChange = { zoom = it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        NavegacaoDePaginas(
            indicePagina = indicePagina,
            totalPaginas = totalPaginas,
            onPaginaAnterior = onPaginaAnterior,
            onProximaPagina = onProximaPagina
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CampoSomenteLeitura(
    campo: CampoVisualizacaoUi,
    larguraDaPagina: Dp,
    alturaDaPagina: Dp
) {
    val cores = LetsFlowTheme.cores
    val formato = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .offset(
                x = larguraDaPagina * campo.posicaoX,
                y = alturaDaPagina * campo.posicaoY
            )
            .size(
                width = larguraDaPagina * campo.largura,
                height = alturaDaPagina * campo.altura
            )
            .clip(formato)
            .background(cores.destaque)
            .border(1.dp, cores.acao, formato)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "✎ ${campo.rotulo}",
            color = cores.acao,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis // Campo pequeno corta o texto com "..."
        )
    }
}

@Composable
private fun NavegacaoDePaginas(
    indicePagina: Int,
    totalPaginas: Int,
    onPaginaAnterior: () -> Unit,
    onProximaPagina: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val temAnterior = indicePagina > 0
    val temProxima = indicePagina < totalPaginas - 1

    // fillMaxWidth com Arrangement.Center deixa "‹ Página X de Y ›" no meio da tela
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            fontSize = 22.sp,
            color = if (temAnterior) cores.texto else cores.textoDesabilitado,
            modifier = Modifier
                .clickable(enabled = temAnterior, role = Role.Button) { onPaginaAnterior() }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Text(
            text = "Página ${indicePagina + 1} de $totalPaginas",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = cores.texto
        )
        Text(
            text = "›",
            fontSize = 22.sp,
            color = if (temProxima) cores.texto else cores.textoDesabilitado,
            modifier = Modifier
                .clickable(enabled = temProxima, role = Role.Button) { onProximaPagina() }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

private const val PROPORCAO_DE_FOLHA_A4 = 0.707f

@Preview(name = "Somente leitura", showBackground = true, showSystemUi = true)
@Composable
private fun VisualizarDocumentoPreview() {
    LetsFlowTheme {
        VisualizarDocumentoAdminScreen(
            tituloDoDocumento = "Contrato de Prestação de Serviços",
            paginaAtual = null,
            indicePagina = 2,
            totalPaginas = 4,
            camposDaPagina = listOf(
                CampoVisualizacaoUi(
                    rotulo = "Mariana · Campo 1 de 2",
                    posicaoX = 0.08f,
                    posicaoY = 0.55f,
                    largura = 0.6f,
                    altura = 0.07f
                )
            ),
            onPaginaAnterior = {},
            onProximaPagina = {},
            onVoltar = {}
        )
    }
}