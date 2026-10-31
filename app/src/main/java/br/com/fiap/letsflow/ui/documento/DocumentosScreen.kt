package br.com.fiap.letsflow.ui.documento

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.R
import br.com.fiap.letsflow.data.local.entity.Documento
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// A tela mostra o documento junto com a contagem de assinaturas, que não fica na entidade
data class DocumentoResumo(
    val documento: Documento,
    val totalSignatarios: Int,
    val assinados: Int
)

private enum class AbaDocumentos(val rotulo: String) {
    TODOS("Todos"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDOS("Concluídos")
}

// Tela inicial do Admin. Recebe a lista pronta para não depender do repository
@Composable
fun DocumentosScreen(
    documentos: List<DocumentoResumo>,
    onNovoDocumento: () -> Unit,
    onAbrirDocumento: (Documento) -> Unit,
    onIrParaSignatarios: () -> Unit,
    onSair: () -> Unit,
    modifier: Modifier = Modifier
) {
    var abaSelecionada by rememberSaveable { mutableStateOf(AbaDocumentos.TODOS) }

    DocumentosConteudo(
        documentos = documentos,
        abaSelecionada = abaSelecionada,
        aoSelecionarAba = { abaSelecionada = it },
        onNovoDocumento = onNovoDocumento,
        onAbrirDocumento = onAbrirDocumento,
        onIrParaSignatarios = onIrParaSignatarios,
        onSair = onSair,
        modifier = modifier
    )
}

// Separado da tela para o Preview poder mostrar cada aba sem estado interno
@Composable
private fun DocumentosConteudo(
    documentos: List<DocumentoResumo>,
    abaSelecionada: AbaDocumentos,
    aoSelecionarAba: (AbaDocumentos) -> Unit,
    onNovoDocumento: () -> Unit,
    onAbrirDocumento: (Documento) -> Unit,
    onIrParaSignatarios: () -> Unit,
    onSair: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    val documentosDaAba = documentos.filter { resumo ->
        when (abaSelecionada) {
            AbaDocumentos.TODOS -> true
            AbaDocumentos.EM_ANDAMENTO ->
                resumo.documento.status == StatusDocumento.ENVIADO ||
                        resumo.documento.status == StatusDocumento.PARCIAL
            AbaDocumentos.CONCLUIDOS -> resumo.documento.status == StatusDocumento.CONCLUIDO
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_letsflow),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LETSFLOW",
                        style = tipografia.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = cores.acao,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Sair",
                        style = tipografia.bodySmall,
                        color = cores.acao,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { onSair() }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Documentos",
                    style = tipografia.headlineMedium,
                    color = cores.texto
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Crie documentos e acompanhe assinaturas.",
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )

                Spacer(modifier = Modifier.height(20.dp))

                BotaoPrimario(
                    texto = "+  Novo documento",
                    onClick = onNovoDocumento,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Documentos",
                    style = tipografia.titleLarge,
                    color = cores.texto
                )

                Spacer(modifier = Modifier.height(8.dp))

                AbasDeDocumentos(
                    abaSelecionada = abaSelecionada,
                    aoSelecionar = aoSelecionarAba
                )
            }

            if (documentosDaAba.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum documento aqui ainda.",
                        style = tipografia.bodyMedium,
                        color = cores.textoSecundario,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            items(documentosDaAba, key = { it.documento.id }) { resumo ->
                LinhaDocumento(
                    resumo = resumo,
                    aoAbrir = { onAbrirDocumento(resumo.documento) }
                )
            }
        }

        BarraDeNavegacao(aoIrParaSignatarios = onIrParaSignatarios)
    }
}

@Composable
private fun AbasDeDocumentos(
    abaSelecionada: AbaDocumentos,
    aoSelecionar: (AbaDocumentos) -> Unit
) {
    val cores = LetsFlowTheme.cores

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            AbaDocumentos.entries.forEach { aba ->
                val selecionada = aba == abaSelecionada

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab) { aoSelecionar(aba) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = aba.rotulo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (selecionada) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (selecionada) cores.acao else cores.textoSecundario,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    // O traço só aparece na aba escolhida
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .then(
                                if (selecionada) Modifier.background(cores.acao) else Modifier
                            )
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(cores.divisao)
        )
    }
}

@Composable
private fun LinhaDocumento(
    resumo: DocumentoResumo,
    aoAbrir: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val documento = resumo.documento

    Column(modifier = Modifier.clickable(role = Role.Button) { aoAbrir() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(cores.destaque, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PDF",
                    color = cores.acao,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = documento.titulo,
                    style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // Título comprido corta com "..."
                )
                Text(
                    text = "${formatarData(documento.dataCriacao)} · " +
                            textoDeSignatarios(resumo.totalSignatarios),
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rotuloDoStatus(documento.status),
                    style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    // ATENÇÃO: o Figma usa laranja em Parcial e Aguardando. Se o tema
                    // tiver uma cor de alerta, troque cores.acao por ela
                    color = if (documento.status == StatusDocumento.CONCLUIDO) {
                        cores.concluido
                    } else {
                        cores.acao
                    },
                    textAlign = TextAlign.End
                )
                Text(
                    text = "${resumo.assinados} de ${resumo.totalSignatarios}",
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(cores.divisao)
        )
    }
}

// Sem ícones por enquanto: o projeto não precisa de dependência extra para o texto
@Composable
private fun BarraDeNavegacao(aoIrParaSignatarios: () -> Unit) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val formato = RoundedCornerShape(12.dp)

    Column(modifier = Modifier.background(cores.superficie)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(cores.divisao)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // A aba atual já é Documentos, por isso o toque não faz nada
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(formato)
                    .background(cores.destaque)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Documentos",
                    style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.acao
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(formato)
                    .clickable(role = Role.Tab) { aoIrParaSignatarios() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Signatários",
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
            }
        }
    }
}

private fun rotuloDoStatus(status: StatusDocumento): String = when (status) {
    StatusDocumento.RASCUNHO -> "Rascunho"
    StatusDocumento.ENVIADO -> "Aguardando"
    StatusDocumento.PARCIAL -> "Parcial"
    StatusDocumento.CONCLUIDO -> "Concluído"
}

private fun textoDeSignatarios(total: Int): String =
    if (total == 1) "1 signatário" else "$total signatários"

// Mostra "28 out": o ponto que algumas versões do Android colocam na abreviação sai
private fun formatarData(milissegundos: Long): String =
    SimpleDateFormat("d MMM", Locale.forLanguageTag("pt-BR"))
        .format(Date(milissegundos))
        .lowercase()
        .replace(".", "")

private fun documentosDeExemplo(): List<DocumentoResumo> = listOf(
    DocumentoResumo(
        documento = Documento(
            id = 1,
            titulo = "Contrato de serviços",
            caminhoPdf = "",
            adminId = 1,
            status = StatusDocumento.PARCIAL,
            dataCriacao = 1761652800000L
        ),
        totalSignatarios = 2,
        assinados = 1
    ),
    DocumentoResumo(
        documento = Documento(
            id = 2,
            titulo = "Termo de autorização",
            caminhoPdf = "",
            adminId = 1,
            status = StatusDocumento.ENVIADO,
            dataCriacao = 1761566400000L
        ),
        totalSignatarios = 1,
        assinados = 0
    ),
    DocumentoResumo(
        documento = Documento(
            id = 3,
            titulo = "Acordo comercial",
            caminhoPdf = "",
            adminId = 1,
            status = StatusDocumento.CONCLUIDO,
            dataCriacao = 1761480000000L
        ),
        totalSignatarios = 2,
        assinados = 2
    )
)

@Preview(name = "Todos", showBackground = true, showSystemUi = true)
@Composable
private fun DocumentosTodosPreview() {
    LetsFlowTheme {
        DocumentosConteudo(
            documentos = documentosDeExemplo(),
            abaSelecionada = AbaDocumentos.TODOS,
            aoSelecionarAba = {},
            onNovoDocumento = {},
            onAbrirDocumento = {},
            onIrParaSignatarios = {},
            onSair = {}
        )
    }
}

@Preview(name = "Em andamento", showBackground = true, showSystemUi = true)
@Composable
private fun DocumentosEmAndamentoPreview() {
    LetsFlowTheme {
        DocumentosConteudo(
            documentos = documentosDeExemplo(),
            abaSelecionada = AbaDocumentos.EM_ANDAMENTO,
            aoSelecionarAba = {},
            onNovoDocumento = {},
            onAbrirDocumento = {},
            onIrParaSignatarios = {},
            onSair = {}
        )
    }
}

@Preview(name = "Concluídos", showBackground = true, showSystemUi = true)
@Composable
private fun DocumentosConcluidosPreview() {
    LetsFlowTheme {
        DocumentosConteudo(
            documentos = documentosDeExemplo(),
            abaSelecionada = AbaDocumentos.CONCLUIDOS,
            aoSelecionarAba = {},
            onNovoDocumento = {},
            onAbrirDocumento = {},
            onIrParaSignatarios = {},
            onSair = {}
        )
    }
}