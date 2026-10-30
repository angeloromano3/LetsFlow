package br.com.fiap.letsflow.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// Os 4 estados de documento do Figma
enum class TipoEtiqueta { AGUARDANDO, PARCIAL, CONCLUIDO, ERRO }

@Composable
fun EtiquetaStatus(texto: String, tipo: TipoEtiqueta, modifier: Modifier = Modifier) {
    val cores = LetsFlowTheme.cores
    val (cor, fundo) = when (tipo) {
        TipoEtiqueta.AGUARDANDO -> cores.acao to cores.destaque
        TipoEtiqueta.PARCIAL -> cores.pendente to cores.fundoPendente
        TipoEtiqueta.CONCLUIDO -> cores.concluido to cores.fundoConcluido
        TipoEtiqueta.ERRO -> cores.erro to cores.fundoErro
    }
    Text(
        texto,
        color = cor,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        modifier = modifier
            .background(fundo, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

// Atalho: etiqueta certa direto do status do banco
@Composable
fun EtiquetaStatus(status: StatusDocumento, modifier: Modifier = Modifier) {
    when (status) {
        StatusDocumento.RASCUNHO -> EtiquetaStatus("Rascunho", TipoEtiqueta.AGUARDANDO, modifier)
        StatusDocumento.ENVIADO -> EtiquetaStatus("Aguardando", TipoEtiqueta.AGUARDANDO, modifier)
        StatusDocumento.PARCIAL -> EtiquetaStatus("Parcial", TipoEtiqueta.PARCIAL, modifier)
        StatusDocumento.CONCLUIDO -> EtiquetaStatus("Concluído", TipoEtiqueta.CONCLUIDO, modifier)
    }
}