package br.com.fiap.letsflow.ui.assinatura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.componentes.BotaoSecundario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// Painel que sobe de baixo para desenhar a assinatura (substitui o antigo QuadroAssinaturaDialog)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelAssinatura(
    onCancelar: () -> Unit,
    onAplicar: (List<List<Offset>>) -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val tracos = remember { mutableStateListOf<List<Offset>>() }
    var tracoAtual by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var limpou by remember { mutableStateOf(false) } // Muda o texto de ajuda depois do Limpar
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true) // Abre já inteiro

    ModalBottomSheet(
        onDismissRequest = onCancelar, // Tocar fora ou no voltar fecha sem aplicar
        sheetState = estado,
        sheetGesturesEnabled = false, // Sem isso, arrastar o dedo para desenhar puxaria o painel para baixo
        dragHandle = null,
        containerColor = cores.superficie
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 32.dp)
        ) {
            Text("Desenhe sua assinatura", style = tipografia.titleLarge, color = cores.texto)
            Spacer(Modifier.height(4.dp))
            Text(
                if (limpou && tracos.isEmpty()) "A área foi limpa. Toque para começar a desenhar."
                else "Use o dedo para assinar no espaço abaixo.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )
            Spacer(Modifier.height(16.dp))

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f) // Mesma proporção 3:1 da imagem salva pelo ArquivoAssinatura
                    .clip(RoundedCornerShape(12.dp)) // Também corta o traço que passar da borda
                    .background(Color.White) // Área sempre branca, como papel, mesmo no modo escuro
                    .border(1.dp, cores.divisao, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { inicio -> tracoAtual = listOf(inicio.normalizar(size)) },
                            onDrag = { change, _ ->
                                change.consume()
                                tracoAtual = tracoAtual + change.position.normalizar(size)
                            },
                            onDragEnd = {
                                tracos += tracoAtual
                                tracoAtual = emptyList()
                            }
                        )
                    }
            ) {
                tracos.forEach { desenharTraco(it) }
                desenharTraco(tracoAtual)
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotaoSecundario(
                    texto = "Limpar",
                    onClick = {
                        tracos.clear()
                        limpou = true
                    },
                    modifier = Modifier.weight(1f)
                )
                // Cinza enquanto não houver desenho
                BotaoPrimario(
                    texto = "Aplicar assinatura",
                    onClick = { onAplicar(tracos.toList()) },
                    modifier = Modifier.weight(1.8f),
                    habilitado = tracos.isNotEmpty()
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "A assinatura será aplicada a todos os seus campos.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )
        }
    }
}

// Prende o ponto entre 0 e 1: se o dedo sair da área, o traço fica colado na borda
private fun Offset.normalizar(size: IntSize) = Offset(
    (x / size.width).coerceIn(0f, 1f),
    (y / size.height).coerceIn(0f, 1f)
)

private fun DrawScope.desenharTraco(pontos: List<Offset>) {
    if (pontos.size < 2) return
    val caminho = Path().apply {
        moveTo(pontos[0].x * size.width, pontos[0].y * size.height)
        pontos.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
    }
    drawPath(
        caminho,
        Color.Black, // Tinta sempre preta, porque o fundo é sempre branco
        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}