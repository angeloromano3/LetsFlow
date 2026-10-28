package br.com.fiap.letsflow.ui.assinatura

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.data.repository.DocumentoRepository

// Lista os documentos enviados para o signatário logado
@Composable
fun DocumentosRecebidosScreen(
    usuarioId: Long,
    onAbrir: (Long) -> Unit // Recebe o id do documento tocado
) {
    val context = LocalContext.current
    val repository = remember {
        val db = AppDatabase.getInstance(context)
        DocumentoRepository(
            db.documentoDao(),
            db.documentoSignatarioDao(),
            db.campoAssinaturaDao(),
            db.eventoDao()
        )
    }

    // Flow: a lista se atualiza sozinha quando o banco muda
    val documentos by repository.listarRecebidos(usuarioId).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Text("Documentos para assinar", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        if (documentos.isEmpty()) {
            Text("Nenhum documento recebido.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(documentos, key = { it.id }) { documento ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onAbrir(documento.id) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(documento.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(textoStatus(documento.status), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// Converte o enum em texto amigável pra tela
private fun textoStatus(status: StatusDocumento) = when (status) {
    StatusDocumento.RASCUNHO -> "Rascunho"
    StatusDocumento.ENVIADO -> "Aguardando assinaturas"
    StatusDocumento.PARCIAL -> "Assinado parcialmente"
    StatusDocumento.CONCLUIDO -> "Concluído"
}