package br.com.fiap.letsflow.ui.documento

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.Documento
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import kotlinx.coroutines.flow.map

// Liga a DocumentosScreen ao banco: busca os documentos do Admin logado e repassa para a tela.
// A tela continua sem conhecer o repository; quem conhece é só esta rota
@Composable
fun DocumentosRota(
    adminId: Long,
    onNovoDocumento: () -> Unit,
    onAbrirDocumento: (Documento) -> Unit,
    onIrParaSignatarios: () -> Unit,
    onSair: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val documentoRepository = remember {
        DocumentoRepository(
            db.documentoDao(),
            db.documentoSignatarioDao(),
            db.campoAssinaturaDao(),
            db.eventoDao()
        )
    }

    // O Flow fica guardado por adminId: sem o remember, cada recomposição criaria um Flow novo
    // e a coleta recomeçaria. Quando um documento é enviado ou assinado, o Room emite a lista de novo
    val resumosFlow = remember(adminId) {
        documentoRepository.listarDoAdmin(adminId).map { lista ->
            lista.map { item ->
                DocumentoResumo(
                    documento = item.documento,
                    totalSignatarios = item.totalSignatarios,
                    assinados = item.assinados
                )
            }
        }
    }
    val documentos by resumosFlow.collectAsState(initial = emptyList())

    DocumentosScreen(
        documentos = documentos,
        onNovoDocumento = onNovoDocumento,
        onAbrirDocumento = onAbrirDocumento,
        onIrParaSignatarios = onIrParaSignatarios,
        onSair = onSair,
        modifier = modifier
    )
}
