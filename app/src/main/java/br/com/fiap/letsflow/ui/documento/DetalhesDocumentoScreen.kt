package br.com.fiap.letsflow.ui.documento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fiap.letsflow.data.local.entity.Documento
import br.com.fiap.letsflow.data.local.entity.Evento
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.data.local.model.SignatarioDoDocumento
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Suporte aos ícones utilizados sem necessidade de alterar as dependências do projeto
private object Icons {
    object AutoMirrored {
        object Filled {
            val ArrowBack: ImageVector by lazy {
                ImageVector.Builder(
                    name = "ArrowBack",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(20f, 11f)
                        horizontalLineTo(7.83f)
                        lineTo(13.42f, 5.41f)
                        lineTo(12f, 4f)
                        lineTo(4f, 12f)
                        lineTo(12f, 20f)
                        lineTo(13.41f, 18.59f)
                        lineTo(7.83f, 13f)
                        horizontalLineTo(20f)
                        close()
                    }
                }.build()
            }
        }
    }

    object Default {
        val CheckCircle: ImageVector by lazy {
            ImageVector.Builder(
                name = "CheckCircle",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12f, 2f)
                    curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                    curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                    curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                    curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                    close()
                    moveTo(10f, 17f)
                    lineTo(5f, 12f)
                    lineTo(6.41f, 10.59f)
                    lineTo(10f, 14.17f)
                    lineTo(17.59f, 6.58f)
                    lineTo(19f, 8f)
                    lineTo(10f, 17f)
                    close()
                }
            }.build()
        }

        val Schedule: ImageVector by lazy {
            ImageVector.Builder(
                name = "Schedule",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(11.99f, 2f)
                    curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
                    curveTo(2f, 17.52f, 6.47f, 22f, 11.99f, 22f)
                    curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                    curveTo(22f, 6.48f, 17.52f, 2f, 11.99f, 2f)
                    close()
                    moveTo(12f, 20f)
                    curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                    curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
                    curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
                    curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
                    close()
                    moveTo(12.5f, 7f)
                    horizontalLineTo(11f)
                    verticalLineTo(13f)
                    lineTo(16.25f, 16.15f)
                    lineTo(17f, 14.92f)
                    lineTo(12.5f, 12.25f)
                    close()
                }
            }.build()
        }
    }
}

private val formatoDataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
private fun formatarDataHora(millis: Long): String = formatoDataHora.format(Date(millis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesDocumentoScreen(
    documentoId: Long,
    documentoRepository: DocumentoRepository,
    onVoltarClick: () -> Unit,
    onVisualizarDocumentoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    var documento by remember { mutableStateOf<Documento?>(null) }
    var signatarios by remember { mutableStateOf<List<SignatarioDoDocumento>>(emptyList()) }
    var eventos by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }

    LaunchedEffect(documentoId) {
        documento = documentoRepository.buscarPorId(documentoId)
        signatarios = documentoRepository.listarSignatarios(documentoId)
        
        val eventosResult = documentoRepository.listarEventos(documentoId)
        eventos = when (eventosResult) {
            is Flow<*> -> (eventosResult.firstOrNull() as? List<Evento>) ?: emptyList()
            is List<*> -> eventosResult.filterIsInstance<Evento>()
            else -> emptyList()
        }
        
        carregando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Detalhes do documento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = documento?.titulo ?: "Carregando...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltarClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onVisualizarDocumentoClick(documentoId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Visualizar documento")
                }
            }
        }
    ) { innerPadding ->
        if (carregando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val totalSignatarios = signatarios.size
            val totalAssinados = signatarios.count { it.assinado }
            val progresso = if (totalSignatarios > 0) totalAssinados.toFloat() / totalSignatarios.toFloat() else 0f

            val pendentes = signatarios.filter { !it.assinado }
            val textoAguardando = when {
                pendentes.isEmpty() && totalSignatarios > 0 -> "Todas as assinaturas concluídas"
                pendentes.size == 1 -> "Aguardando assinatura de ${pendentes.first().nome}"
                pendentes.size > 1 -> "Aguardando assinatura de ${pendentes.first().nome} e outros"
                else -> "Sem signatários cadastrados"
            }

            val statusTexto = when (documento?.status) {
                StatusDocumento.CONCLUIDO -> "Concluído"
                StatusDocumento.PARCIAL -> "Parcialmente assinado"
                StatusDocumento.ENVIADO -> "Aguardando assinaturas"
                StatusDocumento.RASCUNHO -> "Rascunho"
                null -> "Desconhecido"
            }

            val isConcluido = statusTexto == "Concluído"

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(statusTexto) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isConcluido) cores.fundoConcluido else cores.fundoPendente,
                                    labelColor = if (isConcluido) cores.concluido else cores.pendente
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { progresso },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Assinaturas realizadas $totalAssinados de $totalSignatarios",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = textoAguardando,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Participantes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(signatarios) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iniciais = item.nome.trim().split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .joinToString("")
                            .uppercase()

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (iniciais.isNotBlank()) iniciais else "S",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.nome,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = item.papel.rotulo(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (item.assinado) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Assinado",
                                    tint = cores.concluido,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val textoAssinatura = if (item.dataAssinatura != null) {
                                    "Assinado • ${formatarDataHora(item.dataAssinatura)}"
                                } else {
                                    "Assinado"
                                }
                                Text(
                                    text = textoAssinatura,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cores.concluido
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Aguardando assinatura",
                                    tint = cores.pendente,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Aguardando assinatura",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cores.pendente
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Histórico de eventos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Eventos ordenados do mais novo para o mais antigo, conforme o checklist
                val eventosOrdenados = eventos.sortedByDescending { it.dataHora }

                items(eventosOrdenados) { ev ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = ev.descricao,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatarDataHora(ev.dataHora),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
