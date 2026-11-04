package br.com.fiap.letsflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.ui.documento.DetalhesDocumentoScreen
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instancia o banco local Room e o repositório
        val db = AppDatabase.getInstance(applicationContext)
        val repo = DocumentoRepository(
            documentoDao = db.documentoDao(),
            documentoSignatarioDao = db.documentoSignatarioDao(),
            campoAssinaturaDao = db.campoAssinaturaDao(),
            eventoDao = db.eventoDao()
        )

        setContent {
            LetsFlowTheme {
                DetalhesDocumentoScreen(
                    documentoId = 1L,
                    documentoRepository = repo,
                    onVoltarClick = { finish() },
                    onVisualizarDocumentoClick = { docId ->
                        // Ação de clique para navegar para o visualizador de PDF
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
