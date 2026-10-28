package br.com.fiap.letsflow.ui.assinatura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.window.Dialog

@Composable
fun QuadroAssinaturaDialog(
    onCancelar: () -> Unit,
    onConfirmar: (List<List<Offset>>) -> Unit
) {
    val tracos = remember { mutableStateListOf<List<Offset>>() }
    var tracoAtual by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Dialog(onDismissRequest = onCancelar) {
        Column(Modifier.background(Color.White).padding(16.dp)) {
            Text("Desenhe sua assinatura")
            Spacer(Modifier.height(8.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    // Proporção fixa 3:1, igual à imagem salva pelo ArquivoAssinatura
                    .aspectRatio(3f)
                    .border(1.dp, Color.Gray)
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
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { tracos.clear() }) { Text("Limpar") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCancelar) { Text("Cancelar") }
                Button(
                    onClick = { onConfirmar(tracos.toList()) },
                    enabled = tracos.isNotEmpty()
                ) { Text("Confirmar") }
            }
        }
    }
}

private fun Offset.normalizar(size: IntSize) = Offset(x / size.width, y / size.height)

private fun DrawScope.desenharTraco(pontos: List<Offset>) {
    if (pontos.size < 2) return
    val caminho = Path().apply {
        moveTo(pontos[0].x * size.width, pontos[0].y * size.height)
        pontos.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
    }
    drawPath(
        caminho,
        Color.Black,
        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}