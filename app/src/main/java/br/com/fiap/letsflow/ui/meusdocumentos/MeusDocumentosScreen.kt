package br.com.fiap.letsflow.ui.meusdocumentos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// Um documento enviado para o signatário. A tela recebe a lista pronta, sem repository
data class DocumentoDoSignatarioUi(
    val id: Long,
    val titulo: String,
    val dataDeRecebimento: String, // Já formatada, por exemplo "28/10"
    val quantidadeDeCampos: Int,
    val assinado: Boolean // Se a assinatura desta pessoa já foi concluída
)

private enum class AbaMeusDocumentos(val rotulo: String) {
    PENDENTES("PENDENTES"),
    ASSINADOS("ASSINADOS")
}

// Tela inicial do Signatário: os documentos dele separados em pendentes e assinados
@Composable
fun MeusDocumentosScreen(
    nomeDoSignatario: String,
    documentos: List<DocumentoDoSignatarioUi>,
    onAssinar: (DocumentoDoSignatarioUi) -> Unit,
    onVisualizar: (DocumentoDoSignatarioUi) -> Unit,
    onSair: () -> Unit,
    modifier: Modifier = Modifier
) {
    var abaSelecionada by rememberSaveable { mutableStateOf(AbaMeusDocumentos.PENDENTES) }
    var idSelecionado by rememberSaveable { mutableStateOf<Long?>(null) }

    MeusDocumentosConteudo(
        nomeDoSignatario = nomeDoSignatario,
        documentos = documentos,
        abaSelecionada = abaSelecionada,
        idSelecionado = idSelecionado,
        aoSelecionarAba = {
            abaSelecionada = it
            idSelecionado = null // Ao trocar de aba, volta a destacar o primeiro documento
        },
        aoSelecionarDocumento = { idSelecionado = it },
        onAssinar = onAssinar,
        onVisualizar = onVisualizar,
        onSair = onSair,
        modifier = modifier
    )
}

// Separado da tela para o Preview mostrar cada estado do Figma
@Composable
private fun MeusDocumentosConteudo(
    nomeDoSignatario: String,
    documentos: List<DocumentoDoSignatarioUi>,
    abaSelecionada: AbaMeusDocumentos,
    idSelecionado: Long?,
    aoSelecionarAba: (AbaMeusDocumentos) -> Unit,
    aoSelecionarDocumento: (Long) -> Unit,
    onAssinar: (DocumentoDoSignatarioUi) -> Unit,
    onVisualizar: (DocumentoDoSignatarioUi) -> Unit,
    onSair: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    val quantidadeDePendentes = documentos.count { !it.assinado }
    val documentosDaAba = documentos.filter {
        it.assinado == (abaSelecionada == AbaMeusDocumentos.ASSINADOS)
    }
    // O primeiro documento da aba já aparece destacado, com o botão de ação embaixo
    val idDestacado = idSelecionado ?: documentosDaAba.firstOrNull()?.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Meus documentos",
            style = tipografia.headlineMedium,
            color = cores.texto
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Olá, $nomeDoSignatario. Aqui estão suas assinaturas.",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (documentosDaAba.isEmpty()) {
            TituloDaSecao(
                titulo = "Nenhum documento encontrado",
                descricao = "Quando houver um envio destinado a você, ele aparecerá aqui."
            )
        } else {
            if (abaSelecionada == AbaMeusDocumentos.ASSINADOS) {
                TituloDaSecao(
                    titulo = "Assinados",
                    descricao = "Documentos cuja assinatura individual já foi concluída."
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            ContadorDePendentes(quantidade = quantidadeDePendentes)
        }

        Spacer(modifier = Modifier.height(20.dp))

        AbasDeMeusDocumentos(
            abaSelecionada = abaSelecionada,
            aoSelecionar = aoSelecionarAba
        )

        Spacer(modifier = Modifier.height(16.dp))

        documentosDaAba.forEach { documento ->
            val destacado = documento.id == idDestacado

            CartaoDoDocumento(
                documento = documento,
                aoTocar = { aoSelecionarDocumento(documento.id) }
            )

            if (destacado) {
                Spacer(modifier = Modifier.height(12.dp))
                BotaoPrimario(
                    texto = if (documento.assinado) "Visualizar documento" else "Visualizar e assinar",
                    onClick = {
                        if (documento.assinado) onVisualizar(documento) else onAssinar(documento)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "Acesse apenas os documentos enviados para você.",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (documentosDaAba.isEmpty()) {
            // Sem documentos na aba, o link leva de volta para a aba de pendentes
            Text(
                text = "Voltar aos meus documentos",
                style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = cores.acao,
                modifier = Modifier
                    .clickable(role = Role.Button) {
                        aoSelecionarAba(AbaMeusDocumentos.PENDENTES)
                    }
                    .padding(vertical = 4.dp)
            )
        } else {
            Text(
                text = "Sair da conta",
                style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = cores.textoSecundario,
                modifier = Modifier
                    .clickable(role = Role.Button) { onSair() }
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TituloDaSecao(
    titulo: String,
    descricao: String
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    Text(
        text = titulo,
        style = tipografia.titleLarge,
        color = cores.acao
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = descricao,
        style = tipografia.bodySmall,
        color = cores.textoSecundario
    )
}

// Número grande com a quantidade de documentos que ainda esperam a assinatura
@Composable
private fun ContadorDePendentes(quantidade: Int) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    val numero = if (quantidade == 0) "0" else quantidade.toString().padStart(2, '0')
    val legenda = when (quantidade) {
        0 -> "Nenhuma assinatura pendente"
        1 -> "Documento aguardando você"
        else -> "Documentos aguardando você"
    }

    Text(
        text = numero,
        style = tipografia.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
        color = cores.acao
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
        text = legenda,
        style = tipografia.bodySmall,
        color = cores.textoSecundario
    )
}

@Composable
private fun AbasDeMeusDocumentos(
    abaSelecionada: AbaMeusDocumentos,
    aoSelecionar: (AbaMeusDocumentos) -> Unit
) {
    val cores = LetsFlowTheme.cores

    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        AbaMeusDocumentos.entries.forEach { aba ->
            val selecionada = aba == abaSelecionada
            // O Figma marca a aba de assinados escolhida com um ✓
            val sufixo = if (selecionada && aba == AbaMeusDocumentos.ASSINADOS) " ✓" else ""

            Text(
                text = aba.rotulo + sufixo,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (selecionada) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (selecionada) cores.acao else cores.textoSecundario,
                modifier = Modifier
                    .clickable(role = Role.Tab) { aoSelecionar(aba) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CartaoDoDocumento(
    documento: DocumentoDoSignatarioUi,
    aoTocar: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val formato = RoundedCornerShape(12.dp)
    val textoDeCampos = if (documento.quantidadeDeCampos == 1) {
        "1 campo"
    } else {
        "${documento.quantidadeDeCampos} campos"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(formato)
            .background(cores.superficie)
            .border(1.dp, cores.divisao, formato)
            .clickable(role = Role.Button) { aoTocar() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "▤", style = tipografia.bodySmall, color = cores.textoSecundario)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = documento.titulo,
                style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cores.texto
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Recebido em ${documento.dataDeRecebimento} · $textoDeCampos",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (documento.assinado) "Assinado" else "Pendente",
            style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            // ATENÇÃO: no Figma o "Pendente" é laranja. Se o tema tiver uma cor
            // de alerta, troque cores.acao por ela
            color = if (documento.assinado) cores.concluido else cores.acao
        )
    }
}

private fun documentosPendentesDeExemplo() = listOf(
    DocumentoDoSignatarioUi(
        id = 1,
        titulo = "Contrato de Serviços",
        dataDeRecebimento = "28/10",
        quantidadeDeCampos = 2,
        assinado = false
    ),
    DocumentoDoSignatarioUi(
        id = 2,
        titulo = "Termo de Autorização",
        dataDeRecebimento = "27/10",
        quantidadeDeCampos = 1,
        assinado = false
    )
)

private fun documentosAssinadosDeExemplo() =
    documentosPendentesDeExemplo().map { it.copy(assinado = true) }

@Preview(name = "Pendentes", showBackground = true, showSystemUi = true)
@Composable
private fun MeusDocumentosPendentesPreview() {
    LetsFlowTheme {
        MeusDocumentosConteudo(
            nomeDoSignatario = "Mariana",
            documentos = documentosPendentesDeExemplo(),
            abaSelecionada = AbaMeusDocumentos.PENDENTES,
            idSelecionado = null,
            aoSelecionarAba = {},
            aoSelecionarDocumento = {},
            onAssinar = {},
            onVisualizar = {},
            onSair = {}
        )
    }
}

@Preview(name = "Assinados", showBackground = true, showSystemUi = true)
@Composable
private fun MeusDocumentosAssinadosPreview() {
    LetsFlowTheme {
        MeusDocumentosConteudo(
            nomeDoSignatario = "Mariana",
            documentos = documentosAssinadosDeExemplo(),
            abaSelecionada = AbaMeusDocumentos.ASSINADOS,
            idSelecionado = null,
            aoSelecionarAba = {},
            aoSelecionarDocumento = {},
            onAssinar = {},
            onVisualizar = {},
            onSair = {}
        )
    }
}

@Preview(name = "Nenhum documento", showBackground = true, showSystemUi = true)
@Composable
private fun MeusDocumentosVazioPreview() {
    LetsFlowTheme {
        MeusDocumentosConteudo(
            nomeDoSignatario = "Mariana",
            documentos = emptyList(),
            abaSelecionada = AbaMeusDocumentos.PENDENTES,
            idSelecionado = null,
            aoSelecionarAba = {},
            aoSelecionarDocumento = {},
            onAssinar = {},
            onVisualizar = {},
            onSair = {}
        )
    }
}