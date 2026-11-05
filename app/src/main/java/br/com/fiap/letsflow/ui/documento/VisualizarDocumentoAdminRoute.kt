
package br.com.fiap.letsflow.ui.documento

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura
import br.com.fiap.letsflow.data.local.storage.RenderizadorPdf
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VisualizarDocumentoAdminRoute(
    documentoId: Long,
    adminId: Long,
    documentoRepository: DocumentoRepository,
    onVoltar: () -> Unit
) {
    var titulo by remember(documentoId) {
        mutableStateOf("")
    }

    var carregando by remember(documentoId) {
        mutableStateOf(true)
    }

    var erro by remember(documentoId) {
        mutableStateOf<String?>(null)
    }

    var renderizador by remember(documentoId) {
        mutableStateOf<RenderizadorPdf?>(null)
    }

    var paginaAtual by remember(documentoId) {
        mutableIntStateOf(0)
    }

    var imagemPagina by remember(documentoId) {
        mutableStateOf<Bitmap?>(null)
    }

    var campos by remember(documentoId) {
        mutableStateOf<List<CampoAssinatura>>(emptyList())
    }

    var nomes by remember(documentoId) {
        mutableStateOf<Map<Long, String>>(emptyMap())
    }

    var mostrarCampos by remember(documentoId) {
        mutableStateOf(true)
    }

    // Carrega o documento e verifica o proprietário.
    LaunchedEffect(documentoId, adminId) {
        try {
            val documento =
                documentoRepository.buscarPorId(documentoId)

            if (
                documento == null ||
                documento.adminId != adminId
            ) {
                erro = "Documento não encontrado ou acesso negado."
                return@LaunchedEffect
            }

            titulo = documento.titulo

            val signatarios =
                documentoRepository.listarSignatarios(documentoId)

            nomes = signatarios.associate {
                it.documentoSignatarioId to it.nome
            }

            campos =
                documentoRepository.listarCampos(documentoId)

            // Prioriza o PDF final quando estiver disponível.
            val pdfFinal = documento.caminhoPdfFinal
                ?.takeIf { File(it).isFile }

            val caminho = pdfFinal ?: documento.caminhoPdf

            // No PDF final as assinaturas já estão incorporadas.
            mostrarCampos = pdfFinal == null

            renderizador = withContext(Dispatchers.IO) {
                RenderizadorPdf(caminho)
            }

        } catch (e: Exception) {
            erro = e.message
                ?: "Não foi possível abrir o PDF."
        } finally {
            carregando = false
        }
    }

    // Fecha o PDF quando a tela for descartada.
    val leitor = renderizador

    DisposableEffect(leitor) {
        onDispose {
            leitor?.close()
        }
    }

    // Renderiza a página selecionada.
    LaunchedEffect(leitor, paginaAtual) {
        imagemPagina = null

        if (leitor != null) {
            try {
                imagemPagina =
                    leitor.renderizarPagina(paginaAtual)
            } catch (e: Exception) {
                erro = "Não foi possível carregar a página."
            }
        }
    }

    when {
        carregando -> {
            Text("Carregando PDF...")
        }

        erro != null -> {
            Column(Modifier.padding(24.dp)) {
                Text(erro ?: "Erro desconhecido")

                Spacer(Modifier.height(12.dp))

                TextButton(onClick = onVoltar) {
                    Text("Voltar")
                }
            }
        }

        leitor != null -> {

            val camposDaPagina = if (mostrarCampos) {
                campos
                    .filter { it.pagina == paginaAtual }
                    .map { campo ->
                        CampoVisualizacaoUi(
                            rotulo = nomes[
                                campo.documentoSignatarioId
                            ] ?: "Signatário",
                            posicaoX = campo.posicaoX,
                            posicaoY = campo.posicaoY,
                            largura = campo.largura,
                            altura = campo.altura
                        )
                    }
            } else {
                emptyList()
            }

            VisualizarDocumentoScreen(
                tituloDoDocumento = titulo,
                paginaAtual = imagemPagina,
                indicePagina = paginaAtual,
                totalPaginas = leitor.totalPaginas,
                camposDaPagina = camposDaPagina,

                onPaginaAnterior = {
                    if (paginaAtual > 0) {
                        paginaAtual--
                    }
                },

                onProximaPagina = {
                    if (paginaAtual < leitor.totalPaginas - 1) {
                        paginaAtual++
                    }
                },

                onVoltar = onVoltar
            )
        }
    }
}
