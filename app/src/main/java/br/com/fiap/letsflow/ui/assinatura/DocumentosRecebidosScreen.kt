package br.com.fiap.letsflow.ui.assinatura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.model.DocumentoRecebido
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.data.repository.UsuarioRepository
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Lista os documentos recebidos pelo signatário, separados nas abas Pendentes e Assinados
@Composable
fun DocumentosRecebidosScreen(
    usuarioId: Long,
    onAssinar: (documentoId: Long) -> Unit,
    onVisualizar: (documentoId: Long) -> Unit,
    onSair: () -> Unit
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
    val usuarioRepository = remember { UsuarioRepository(db.usuarioDao()) }
    val cores = LetsFlowTheme.cores // Paleta clara ou escura, conforme o sistema
    val tipografia = MaterialTheme.typography // Estilos de texto do Figma

    // Lista do banco que se atualiza sozinha quando alguém assina
    val documentos by documentoRepository.listarRecebidos(usuarioId)
        .collectAsState(initial = emptyList())

    var primeiroNome by remember { mutableStateOf("") }
    LaunchedEffect(usuarioId) {
        primeiroNome =
            usuarioRepository.buscarPorId(usuarioId)?.nomeCompleto?.substringBefore(" ").orEmpty()
    }

    // rememberSaveable mantém a aba escolhida mesmo se girar a tela
    var abaAssinados by rememberSaveable { mutableStateOf(false) }
    var selecionadoId by remember { mutableStateOf<Long?>(null) }

    val pendentes = documentos.filter { !it.assinado }
    val assinados = documentos.filter { it.assinado }
    val lista = if (abaAssinados) assinados else pendentes
    // Se nenhum cartão foi tocado ainda, o primeiro da lista fica selecionado
    val selecionado = lista.find { it.documentoId == selecionadoId } ?: lista.firstOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("Meus documentos", style = tipografia.headlineMedium, color = cores.texto)
        Spacer(Modifier.height(24.dp))
        Text(
            "Olá, $primeiroNome. Aqui estão suas assinaturas.",
            style = tipografia.bodyMedium,
            color = cores.textoSecundario
        )
        Spacer(Modifier.height(16.dp))

        if (documentos.isEmpty()) {
            Text("Nenhum documento encontrado", style = tipografia.titleMedium, color = cores.acao)
            Text(
                "Quando houver um envio destinado a você, ele aparecerá aqui.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )
        } else {
            val total = if (abaAssinados) assinados.size else pendentes.size
            Text("%02d".format(total), style = tipografia.headlineMedium, color = cores.acao)
            Text(
                if (abaAssinados) "Documentos já assinados por você" else "Documentos aguardando você",
                style = tipografia.bodyMedium,
                color = cores.textoSecundario
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Aba("PENDENTES", ativa = !abaAssinados) { abaAssinados = false }
            Aba("ASSINADOS", ativa = abaAssinados) { abaAssinados = true }
        }
        Spacer(Modifier.height(16.dp))

        if (documentos.isNotEmpty() && lista.isEmpty()) {
            Text(
                if (abaAssinados) "Você ainda não assinou nenhum documento." else "Nenhuma assinatura pendente.",
                style = tipografia.bodyMedium,
                color = cores.textoSecundario
            )
            Spacer(Modifier.height(16.dp))
        }

        // Um cartão por documento; só o selecionado mostra o botão de ação
        lista.forEach { documento ->
            key(documento.documentoId) {
                val ehSelecionado = documento.documentoId == selecionado?.documentoId
                CartaoDocumento(documento, ehSelecionado) { selecionadoId = documento.documentoId }

                if (ehSelecionado) {
                    Spacer(Modifier.height(16.dp))
                    BotaoPrimario(
                        texto = if (documento.assinado) "Visualizar documento" else "Visualizar e assinar",
                        onClick = {
                            if (documento.assinado) onVisualizar(documento.documentoId)
                            else onAssinar(documento.documentoId)
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Text(
            "Acesse apenas os documentos enviados para você.",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Sair da conta",
            style = tipografia.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = cores.texto,
            modifier = Modifier
                .clickable(onClick = onSair)
                .padding(vertical = 4.dp)
        )
    }
}

// Aba do topo: azul e com linha embaixo quando ativa, cinza quando não
@Composable
private fun Aba(texto: String, ativa: Boolean, onClick: () -> Unit) {
    val cores = LetsFlowTheme.cores
    Column(Modifier
        .width(IntrinsicSize.Max)
        .clickable(onClick = onClick)) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (ativa) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (ativa) cores.acao else cores.textoSecundario,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (ativa) cores.acao else Color.Transparent)
        )
    }
}

// Cartão com título, data, número de campos e situação; borda azul quando selecionado
@Composable
private fun CartaoDocumento(
    documento: DocumentoRecebido,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val formato = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(formato)
            .background(cores.superficie)
            .border(
                width = if (selecionado) 1.5.dp else 1.dp,
                color = if (selecionado) cores.acao else cores.divisao,
                shape = formato
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            "▤  ${documento.titulo}",
            style = tipografia.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = cores.texto
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Recebido em ${formatarData(documento.dataRecebimento)} · ${textoCampos(documento.totalCampos)}",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (documento.assinado) "Assinado" else "Pendente",
            style = tipografia.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (documento.assinado) cores.concluido else cores.pendente
        )
    }
}

// Formata a data com dia, mês
private val formatoData = SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR"))

private fun formatarData(millis: Long) = formatoData.format(Date(millis))

// "1 campo" no singular, "2 campos" no plural
private fun textoCampos(total: Int) = if (total == 1) "1 campo" else "$total campos"