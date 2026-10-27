package br.com.fiap.letsflow.ui.documento

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.Papel
import br.com.fiap.letsflow.data.local.storage.ArquivoPdf
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

// Tela onde o Admin informa o título, escolhe o PDF e os signatários com o papel de cada um
@Composable
fun NovoDocumentoScreen(
    adminId: Long,
    onSalvo: (documentoId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositórios criados uma vez só; depois isso vai para um ViewModel
    val db = remember { AppDatabase.getInstance(context) }
    val usuarioRepository = remember { UsuarioRepository(db.usuarioDao()) }
    val documentoRepository = remember {
        DocumentoRepository(
            db.documentoDao(),
            db.documentoSignatarioDao(),
            db.campoAssinaturaDao(),
            db.eventoDao())
    }
    val arquivoPdf = remember { ArquivoPdf(context) }

    var titulo by remember { mutableStateOf("") }
    var caminhoPdf by remember { mutableStateOf<String?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }

    // Signatários marcados: id do usuário -> papel
    val selecionados = remember { mutableStateMapOf<Long, Papel>() }

    // A lista atualiza sozinha por causa do Flow
    val signatarios by usuarioRepository.listarSignatarios().collectAsState(initial = emptyList())

    // Abre o seletor de arquivos do Android, filtrando só PDF
    val escolherPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                arquivoPdf.salvar(uri)
                    .onSuccess { caminhoPdf = it; mensagem = null }
                    .onFailure { mensagem = it.message }
            }
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Novo documento", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título do documento") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { escolherPdf.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (caminhoPdf == null) "Selecionar PDF" else "PDF selecionado ✓ (trocar)")
        }
        Spacer(Modifier.height(16.dp))

        Text("Signatários", style = MaterialTheme.typography.titleMedium)

        LazyColumn(Modifier.weight(1f)) {
            items(signatarios, key = { it.id }) { usuario ->
                val papel = selecionados[usuario.id]
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Marcar já coloca como contratante; desmarcar remove
                            Checkbox(
                                checked = papel != null,
                                onCheckedChange = { marcado ->
                                    if (marcado) selecionados[usuario.id] = Papel.CONTRATANTE
                                    else selecionados.remove(usuario.id)
                                }
                            )
                            Column {
                                Text(usuario.nomeCompleto)
                                Text(usuario.email, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        // Os papéis só aparecem pra quem está marcado
                        if (papel != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Papel.entries.forEach { opcao ->
                                    FilterChip(
                                        selected = papel == opcao,
                                        onClick = { selecionados[usuario.id] = opcao },
                                        label = { Text(opcao.name.lowercase().
                                        replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        mensagem?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val caminho = caminhoPdf
                if (caminho == null) {
                    mensagem = "Selecione um PDF"
                    return@Button
                }
                scope.launch {
                    documentoRepository.salvarRascunho(titulo, caminho,
                        adminId,
                        selecionados.toMap())
                        .onSuccess { onSalvo(it) }
                        .onFailure { mensagem = it.message }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar")
        }
    }
}