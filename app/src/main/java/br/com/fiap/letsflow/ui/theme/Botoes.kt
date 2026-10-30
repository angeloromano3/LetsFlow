package br.com.fiap.letsflow.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

private val formatoBotao = RoundedCornerShape(12.dp)

// Botão "Primário" do Figma
@Composable
fun BotaoPrimario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    val cores = LetsFlowTheme.cores
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = formatoBotao,
        colors = ButtonDefaults.buttonColors(
            containerColor = cores.acao,
            contentColor = cores.textoSobreAcao,
            disabledContainerColor = cores.fundoDesabilitado,
            disabledContentColor = cores.textoDesabilitado
        ),
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) { Text(texto) }
}

// Botão "Secundário" do Figma: superfície com borda de divisão
@Composable
fun BotaoSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    val cores = LetsFlowTheme.cores
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = formatoBotao,
        border = BorderStroke(1.dp, cores.divisao),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = cores.superficie,
            contentColor = cores.texto,
            disabledContainerColor = cores.fundoDesabilitado,
            disabledContentColor = cores.textoDesabilitado
        ),
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) { Text(texto) }
}