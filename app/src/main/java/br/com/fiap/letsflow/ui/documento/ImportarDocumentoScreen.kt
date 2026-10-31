package br.com.fiap.letsflow.ui.documento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.componentes.BotaoSecundario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// PDF já escolhido e validado, com os dados que aparecem no card
data class PdfSelecionadoUi(
    val nome: String,
    val totalPaginas: Int,
    val tamanho: String // Já formatado, por exemplo "1,2 MB"
)

// Um arquivo da lista "Arquivos recentes" do seletor
data class PdfRecenteUi(
    val nome: String,
    val descricao: String // Por exemplo "PDF válido · 4 páginas"
)

// Etapa 1 do fluxo: título do documento e escolha do PDF. Os dados do PDF vêm por parâmetro
@Composable
fun ImportarDocumentoScreen(
    pdfEscolhido: PdfSelecionadoUi?,
    pdfInvalido: Boolean,
    mostrarSeletor: Boolean,
    arquivosRecentes: List<PdfRecenteUi>,
    onSelecionarPdf: () -> Unit,
    onEscolherRecente: (PdfRecenteUi) -> Unit,
    onSubstituir: () -> Unit,
    onRemover: () -> Unit,
    onContinuar: (titulo: String) -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    tituloInicial: String = ""
) {
    var titulo by rememberSaveable { mutableStateOf(tituloInicial) }

    ImportarDocumentoConteudo(
        titulo = titulo,
        pdfEscolhido = pdfEscolhido,
        pdfInvalido = pdfInvalido,
        mostrarSeletor = mostrarSeletor,
        arquivosRecentes = arquivosRecentes,
        aoMudarTitulo = { titulo = it },
        onSelecionarPdf = onSelecionarPdf,
        onEscolherRecente = onEscolherRecente,
        onSubstituir = onSubstituir,
        onRemover = onRemover,
        onContinuar = { onContinuar(titulo.trim()) },
        onVoltar = onVoltar,
        modifier = modifier
    )
}

// Separado da tela para o Preview mostrar cada estado do Figma
@Composable
private fun ImportarDocumentoConteudo(
    titulo: String,
    pdfEscolhido: PdfSelecionadoUi?,
    pdfInvalido: Boolean,
    mostrarSeletor: Boolean,
    arquivosRecentes: List<PdfRecenteUi>,
    aoMudarTitulo: (String) -> Unit,
    onSelecionarPdf: () -> Unit,
    onEscolherRecente: (PdfRecenteUi) -> Unit,
    onSubstituir: () -> Unit,
    onRemover: () -> Unit,
    onContinuar: () -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    // Continuar só libera com título e um PDF válido
    val podeContinuar = titulo.isNotBlank() && pdfEscolhido != null && !pdfInvalido

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            CabecalhoNovoDocumento(onVoltar = onVoltar)

            Spacer(modifier = Modifier.height(16.dp))

            if (pdfInvalido) {
                AvisoDePdfInvalido()
                Spacer(modifier = Modifier.height(16.dp))
            } else if (pdfEscolhido == null && !mostrarSeletor) {
                AvisoDeArquivoNaoSelecionado()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Título do documento *",
                style = tipografia.labelLarge,
                color = cores.texto
            )

            Spacer(modifier = Modifier.height(8.dp))

            CampoDoTitulo(valor = titulo, aoMudarValor = aoMudarTitulo)

            Spacer(modifier = Modifier.height(16.dp))

            CartaoImportarPdf(onSelecionarPdf = onSelecionarPdf)

            if (pdfEscolhido != null) {
                Spacer(modifier = Modifier.height(12.dp))
                CartaoPdfEscolhido(
                    pdf = pdfEscolhido,
                    onSubstituir = onSubstituir,
                    onRemover = onRemover
                )
            }

            if (mostrarSeletor) {
                Spacer(modifier = Modifier.height(12.dp))
                CartaoSeletorDePdf(
                    arquivosRecentes = arquivosRecentes,
                    onEscolherRecente = onEscolherRecente
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Apenas PDFs válidos podem avançar. Arquivos corrompidos serão recusados.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Botão fixo no rodapé, fora da rolagem
        BotaoPrimario(
            texto = "Continuar →",
            onClick = onContinuar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            habilitado = podeContinuar
        )
    }
}

@Composable
private fun CabecalhoNovoDocumento(onVoltar: () -> Unit) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "←",
            fontSize = 22.sp,
            color = cores.texto,
            modifier = Modifier
                .semantics { contentDescription = "Voltar" }
                .clip(CircleShape)
                .clickable(role = Role.Button) { onVoltar() }
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Novo documento",
            style = tipografia.headlineMedium,
            color = cores.texto
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Etapa 1 de 4 · Documento",
        style = tipografia.bodySmall,
        color = cores.textoSecundario
    )
}

@Composable
private fun AvisoDeArquivoNaoSelecionado() {
    val cores = LetsFlowTheme.cores

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cores.superficie)
            .padding(12.dp)
    ) {
        Text(
            text = "Arquivo ainda não selecionado",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = cores.acao
        )
        Text(
            text = "Importe um PDF para habilitar a próxima etapa.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )
    }
}

@Composable
private fun AvisoDePdfInvalido() {
    val cores = LetsFlowTheme.cores

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cores.fundoErro)
            .padding(12.dp)
    ) {
        Text(
            text = "Não foi possível ler o PDF",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = cores.erro
        )
        Text(
            text = "O arquivo está corrompido ou não é um PDF válido. Escolha outro.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )
    }
}

@Composable
private fun CampoDoTitulo(
    valor: String,
    aoMudarValor: (String) -> Unit
) {
    val cores = LetsFlowTheme.cores
    val formaDoCampo = RoundedCornerShape(12.dp)

    BasicTextField(
        value = valor,
        onValueChange = aoMudarValor,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = cores.texto),
        cursorBrush = SolidColor(cores.acao),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(cores.superficie, formaDoCampo)
            .border(1.dp, cores.divisao, formaDoCampo)
            .padding(horizontal = 16.dp),
        decorationBox = { campoInterno ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (valor.isEmpty()) {
                    Text(
                        text = "Ex.: Contrato de prestação de serviços",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.textoSecundario
                    )
                }
                campoInterno()
            }
        }
    )
}

@Composable
private fun CartaoImportarPdf(onSelecionarPdf: () -> Unit) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val formato = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(formato)
            .background(cores.superficie)
            .border(1.dp, cores.divisao, formato)
            .padding(16.dp)
    ) {
        Text(
            text = "↥ Importar arquivo PDF",
            style = tipografia.titleLarge,
            color = cores.acao
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Selecione um PDF armazenado no seu celular.",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(12.dp))

        BotaoSecundario(
            texto = "Selecionar PDF",
            onClick = onSelecionarPdf,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CartaoPdfEscolhido(
    pdf: PdfSelecionadoUi,
    onSubstituir: () -> Unit,
    onRemover: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val formato = RoundedCornerShape(16.dp)
    val textoDePaginas = if (pdf.totalPaginas == 1) "1 página" else "${pdf.totalPaginas} páginas"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(formato)
            .background(cores.superficie)
            .border(1.dp, cores.divisao, formato)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "▤", style = tipografia.bodySmall, color = cores.textoSecundario)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = pdf.nome,
                style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cores.texto
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = textoDePaginas, style = tipografia.bodySmall, color = cores.concluido)
            Text(text = "·", style = tipografia.bodySmall, color = cores.textoSecundario)
            Text(text = pdf.tamanho, style = tipografia.bodySmall, color = cores.textoSecundario)
            Text(text = "·", style = tipografia.bodySmall, color = cores.textoSecundario)
            Text(text = "Arquivo válido", style = tipografia.bodySmall, color = cores.concluido)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Substituir arquivo",
                style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = cores.acao,
                modifier = Modifier.clickable(role = Role.Button) { onSubstituir() }
            )
            Text(text = "·", style = tipografia.bodySmall, color = cores.textoSecundario)
            Text(
                text = "Remover",
                style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = cores.erro,
                modifier = Modifier.clickable(role = Role.Button) { onRemover() }
            )
        }
    }
}

@Composable
private fun CartaoSeletorDePdf(
    arquivosRecentes: List<PdfRecenteUi>,
    onEscolherRecente: (PdfRecenteUi) -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val formato = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(formato)
            .background(cores.superficie)
            .padding(16.dp)
    ) {
        Text(
            text = "Selecionar arquivo PDF",
            style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = cores.texto
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Arquivos recentes · armazenamento do celular",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        arquivosRecentes.forEach { arquivo ->
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cores.fundo)
                    .clickable(role = Role.Button) { onEscolherRecente(arquivo) }
                    .padding(12.dp)
            ) {
                Text(
                    text = arquivo.nome,
                    style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.acao
                )
                Text(
                    text = arquivo.descricao,
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
            }
        }
    }
}

private fun pdfDeExemplo() = PdfSelecionadoUi(
    nome = "contrato_servicos.pdf",
    totalPaginas = 4,
    tamanho = "1,2 MB"
)

private fun arquivosRecentesDeExemplo() = listOf(
    PdfRecenteUi(nome = "contrato_servicos.pdf", descricao = "PDF válido · 4 páginas"),
    PdfRecenteUi(nome = "documento_corrompido.pdf", descricao = "Arquivo PDF · Não foi possível ler")
)

@Preview(name = "PDF selecionado", showBackground = true, showSystemUi = true)
@Composable
private fun ImportarPdfSelecionadoPreview() {
    LetsFlowTheme {
        ImportarDocumentoConteudo(
            titulo = "Contrato de Prestação de Serviços",
            pdfEscolhido = pdfDeExemplo(),
            pdfInvalido = false,
            mostrarSeletor = false,
            arquivosRecentes = arquivosRecentesDeExemplo(),
            aoMudarTitulo = {},
            onSelecionarPdf = {},
            onEscolherRecente = {},
            onSubstituir = {},
            onRemover = {},
            onContinuar = {},
            onVoltar = {}
        )
    }
}

@Preview(name = "Nenhum PDF", showBackground = true, showSystemUi = true)
@Composable
private fun ImportarNenhumPdfPreview() {
    LetsFlowTheme {
        ImportarDocumentoConteudo(
            titulo = "Contrato de Prestação de Serviços",
            pdfEscolhido = null,
            pdfInvalido = false,
            mostrarSeletor = false,
            arquivosRecentes = arquivosRecentesDeExemplo(),
            aoMudarTitulo = {},
            onSelecionarPdf = {},
            onEscolherRecente = {},
            onSubstituir = {},
            onRemover = {},
            onContinuar = {},
            onVoltar = {}
        )
    }
}

@Preview(name = "Escolher arquivo", showBackground = true, showSystemUi = true)
@Composable
private fun ImportarEscolherArquivoPreview() {
    LetsFlowTheme {
        ImportarDocumentoConteudo(
            titulo = "Contrato de Prestação de Serviços",
            pdfEscolhido = null,
            pdfInvalido = false,
            mostrarSeletor = true,
            arquivosRecentes = arquivosRecentesDeExemplo(),
            aoMudarTitulo = {},
            onSelecionarPdf = {},
            onEscolherRecente = {},
            onSubstituir = {},
            onRemover = {},
            onContinuar = {},
            onVoltar = {}
        )
    }
}

@Preview(name = "PDF inválido", showBackground = true, showSystemUi = true)
@Composable
private fun ImportarPdfInvalidoPreview() {
    LetsFlowTheme {
        ImportarDocumentoConteudo(
            titulo = "Contrato de Prestação de Serviços",
            pdfEscolhido = null,
            pdfInvalido = true,
            mostrarSeletor = false,
            arquivosRecentes = arquivosRecentesDeExemplo(),
            aoMudarTitulo = {},
            onSelecionarPdf = {},
            onEscolherRecente = {},
            onSubstituir = {},
            onRemover = {},
            onContinuar = {},
            onVoltar = {}
        )
    }
}