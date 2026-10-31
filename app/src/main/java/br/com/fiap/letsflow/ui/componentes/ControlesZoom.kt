package br.com.fiap.letsflow.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Limites e passo do zoom, usados pelas telas que mostram PDF
const val ZOOM_MINIMO = 1f
const val ZOOM_MAXIMO = 3f
const val PASSO_ZOOM = 0.5f

// Tempo que a porcentagem fica visível depois do último toque
private const val TEMPO_PORCENTAGEM_MS = 1500L

// Controle discreto de zoom: dois quadradinhos "− +" e a porcentagem que aparece só quando usado
@Composable
fun ControlesZoom(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val formato = RoundedCornerShape(10.dp)

    // Conta os toques; cada toque novo reinicia o tempo da porcentagem na tela
    var toques by remember { mutableIntStateOf(0) }
    var mostrarPorcentagem by remember { mutableStateOf(false) }
    LaunchedEffect(toques) {
        if (toques > 0) {
            mostrarPorcentagem = true
            delay(TEMPO_PORCENTAGEM_MS) // Se houver outro toque antes, este espera é cancelado e recomeça
            mostrarPorcentagem = false
        }
    }

    Column(modifier, horizontalAlignment = Alignment.End) {
        // Etiqueta da porcentagem, com fade ao aparecer e sumir
        AnimatedVisibility(visible = mostrarPorcentagem, enter = fadeIn(), exit = fadeOut()) {
            Text(
                "${(zoom * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cores.textoSobreAcao,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .background(cores.acao.copy(alpha = 0.9f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Os dois quadradinhos colados, como um controle só
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .shadow(2.dp, formato) // Sombra leve, só para destacar do PDF
                .clip(formato)
                .background(cores.superficie)
                .border(1.dp, cores.divisao, formato)
        ) {
            BotaoZoom("−", habilitado = zoom > ZOOM_MINIMO) {
                onZoomChange((zoom - PASSO_ZOOM).coerceAtLeast(ZOOM_MINIMO))
                toques++
            }
            // Linha fina separando os dois botões
            Box(
                Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(cores.divisao)
            )
            BotaoZoom("+", habilitado = zoom < ZOOM_MAXIMO) {
                onZoomChange((zoom + PASSO_ZOOM).coerceAtMost(ZOOM_MAXIMO))
                toques++
            }
        }
    }
}

// Quadradinho de 36dp com o símbolo; cinza quando chega no limite do zoom
@Composable
private fun BotaoZoom(simbolo: String, habilitado: Boolean, onClick: () -> Unit) {
    val cores = LetsFlowTheme.cores
    Box(
        Modifier
            .size(36.dp)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            simbolo,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (habilitado) cores.acao else cores.textoDesabilitado
        )
    }
}