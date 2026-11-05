package br.com.fiap.letsflow.ui.documento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.entity.Documento
import br.com.fiap.letsflow.data.local.entity.Evento
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.data.local.model.SignatarioDoDocumento
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.ui.componentes.BotaoSecundario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        containerColor = cores.fundo,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Detalhes do documento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cores.texto
                        )
                        Text(
                            text = documento?.titulo ?: "Carregando...",
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.textoSecundario
                        )
                    }
                },
                navigationIcon = {
                    Text(
                        text = "←",
                        fontSize = 24.sp,
                        color = cores.texto,
                        modifier = Modifier
                            .semantics { contentDescription = "Voltar" }
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onVoltarClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cores.fundo
                )
            )
        },
        bottomBar = {
            Surface(
                color = cores.superficie,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    BotaoSecundario(
                        texto = "Visualizar documento",
                        onClick = { onVisualizarDocumentoClick(documentoId) }
                    )
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
                CircularProgressIndicator(color = cores.acao)
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
            val corEtiqueta = if (isConcluido) cores.concluido else cores.pendente
            val fundoEtiqueta = if (isConcluido) cores.fundoConcluido else cores.fundoPendente

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
                        colors = CardDefaults.cardColors(containerColor = cores.superficie),
                        border = BorderStroke(1.dp, cores.divisao)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(statusTexto) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = fundoEtiqueta,
                                    labelColor = corEtiqueta
                                ),
                                border = null
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { progresso },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = cores.acao,
                                trackColor = cores.divisao
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Assinaturas realizadas $totalAssinados de $totalSignatarios",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = cores.texto
                            )

                            Text(
                                text = textoAguardando,
                                style = MaterialTheme.typography.bodySmall,
                                color = cores.textoSecundario
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Participantes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cores.texto
                    )
                }

                items(signatarios) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iniciaisTexto = iniciais(item.nome).ifBlank { "S" }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(cores.destaque),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = iniciaisTexto,
                                color = cores.acao,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.nome,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = cores.texto
                            )
                            Text(
                                text = item.papel.rotulo(),
                                style = MaterialTheme.typography.bodySmall,
                                color = cores.textoSecundario
                            )
                        }

                        if (item.assinado) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "✓",
                                    color = cores.concluido,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
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
                                Text(
                                    text = "○",
                                    color = cores.pendente,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
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
                    HorizontalDivider(color = cores.divisao)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Histórico de eventos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cores.texto
                    )
                }

                val eventosOrdenados = eventos.sortedByDescending { it.dataHora }

                items(eventosOrdenados) { ev ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = ev.descricao,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cores.texto
                        )
                        Text(
                            text = formatarDataHora(ev.dataHora),
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.textoSecundario
                        )
                    }
                    HorizontalDivider(color = cores.divisao.copy(alpha = 0.5f))
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
