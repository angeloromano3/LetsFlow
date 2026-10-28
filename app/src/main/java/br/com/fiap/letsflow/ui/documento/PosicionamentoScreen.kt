package br.com.fiap.letsflow.ui.documento

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura
import br.com.fiap.letsflow.data.local.entity.Papel
import br.com.fiap.letsflow.data.local.entity.Usuario
import br.com.fiap.letsflow.data.local.model.SignatarioDoDocumento
import br.com.fiap.letsflow.data.local.storage.RenderizadorPdf
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Cores fixas por posição do signatário na lista (substitui as cores dos testSigners)
private val coresSignatarios = listOf(
    Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047), Color(0xFFFB8C00), Color(0xFF8E24AA)
)

private fun corDoIndice(indice: Int) = coresSignatarios[indice % coresSignatarios.size]

// Nome do papel pra mostrar na tela
private fun Papel.rotulo() = when (this) {
    Papel.CONTRATANTE -> "Contratante"
    Papel.CONTRATADO -> "Contratado"
    Papel.TESTEMUNHA -> "Testemunha"
}

@Composable
fun PosicionamentoScreen(
    documentoId: Long,          // Vem da tela de novo documento
    onContinuar: () -> Unit     // Chamado depois que as posições são salvas
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Monta o repositório a partir do banco único do app
    val repository = remember {
        val db = AppDatabase.getInstance(context)
        DocumentoRepository(
            db.documentoDao(),
            db.documentoSignatarioDao(),
            db.campoAssinaturaDao(),
            db.eventoDao()
        )
    }

    var renderizador by remember { mutableStateOf<RenderizadorPdf?>(null) }
    val signatarios = remember { mutableStateListOf<SignatarioDoDocumento>() }
    val campos = remember { mutableStateListOf<CampoAssinatura>() }
    var selecionado by remember { mutableStateOf<SignatarioDoDocumento?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    // Usuários que ainda podem ser adicionados ao documento
    val disponiveis = remember { mutableStateListOf<Usuario>() }
    // Controla a janela de edição de signatários
    var mostrarSignatarios by remember { mutableStateOf(false) }

    // Recarrega do banco a lista de signatários e a de disponíveis, mantendo a seleção se ainda existir
    suspend fun recarregarSignatarios() {
        signatarios.clear()
        signatarios.addAll(repository.listarSignatarios(documentoId))
        disponiveis.clear()
        disponiveis.addAll(repository.listarDisponiveis(documentoId))
        selecionado = signatarios.find { it.documentoSignatarioId == selecionado?.documentoSignatarioId }
            ?: signatarios.firstOrNull()
    }

    // Carrega documento, signatários e campos já salvos do banco
    LaunchedEffect(documentoId) {
        val documento = repository.buscarPorId(documentoId)
        if (documento == null) {
            erro = "Documento não encontrado"
            return@LaunchedEffect
        }
        renderizador = RenderizadorPdf(documento.caminhoPdf)
        recarregarSignatarios() // Agora também carrega os disponíveis
        campos.addAll(repository.listarCampos(documentoId))
    }

    // Fecha o PdfRenderer quando sai da tela
    val atual = renderizador
    DisposableEffect(atual) {
        onDispose { atual?.close() }
    }

    // Janela de adicionar e remover signatários
    if (mostrarSignatarios) {
        SignatariosDialog(
            signatarios = signatarios,
            disponiveis = disponiveis,
            onRemover = { signatario ->
                scope.launch {
                    repository.removerSignatario(documentoId, signatario.documentoSignatarioId)
                        .onSuccess {
                            // Tira da tela só os campos dele; os outros mantêm as posições ainda não salvas
                            campos.removeAll { it.documentoSignatarioId == signatario.documentoSignatarioId }
                            recarregarSignatarios()
                            erro = null
                        }
                        .onFailure { erro = it.message }
                }
            },
            onAdicionar = { usuario, papel ->
                scope.launch {
                    repository.adicionarSignatario(documentoId, usuario.id, papel)
                        .onSuccess {
                            recarregarSignatarios()
                            erro = null
                        }
                        .onFailure { erro = it.message }
                }
            },
            onFechar = { mostrarSignatarios = false }
        )
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Text(
            "Toque na página para criar um campo. Segure para apagar.",
            Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        // Abre a janela de edição, mostrando quantos signatários há
        OutlinedButton(
            onClick = { mostrarSignatarios = true },
            modifier = Modifier.padding(horizontal = 8.dp)
        ) { Text("Signatários (${signatarios.size})") }

        // Seletor de quem vai assinar os próximos campos, agora com os signatários reais
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            signatarios.forEachIndexed { indice, signatario ->
                val cor = corDoIndice(indice)
                val ativo = signatario.documentoSignatarioId == selecionado?.documentoSignatarioId
                Text(
                    text = signatario.nome,
                    color = if (ativo) Color.White else cor,
                    modifier = Modifier
                        .background(if (ativo) cor else Color.Transparent, RoundedCornerShape(50))
                        .border(2.dp, cor, RoundedCornerShape(50))
                        .clickable { selecionado = signatario }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        erro?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }

        if (atual != null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(atual.totalPaginas) { indice ->
                    PaginaItem(
                        renderizador = atual,
                        indice = indice,
                        documentoId = documentoId,
                        campos = campos,
                        signatarios = signatarios,
                        selecionado = selecionado,
                        repository = repository
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Substitui o "Logar campos": salva todas as posições finais de uma vez
        Button(
            onClick = {
                scope.launch {
                    val semCampo = signatarios.filter { s ->
                        campos.none { it.documentoSignatarioId == s.documentoSignatarioId }
                    }
                    if (semCampo.isNotEmpty()) {
                        erro = "Falta campo para: ${semCampo.joinToString { it.nome }}"
                        return@launch
                    }
                    repository.salvarPosicoes(campos.toList())
                    onContinuar()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) { Text("Continuar") }
    }
}

// Janela com os signatários atuais (remover) e os disponíveis (adicionar com papel)
@Composable
private fun SignatariosDialog(
    signatarios: List<SignatarioDoDocumento>,
    disponiveis: List<Usuario>,
    onRemover: (SignatarioDoDocumento) -> Unit,
    onAdicionar: (Usuario, Papel) -> Unit,
    onFechar: () -> Unit
) {
    // Papel que será usado no próximo "Adicionar"
    var papel by remember { mutableStateOf(Papel.CONTRATANTE) }

    AlertDialog(
        onDismissRequest = onFechar,
        confirmButton = { TextButton(onClick = onFechar) { Text("Fechar") } },
        title = { Text("Signatários") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("No documento", style = MaterialTheme.typography.titleSmall)
                signatarios.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${s.nome} (${s.papel.rotulo()})", Modifier.weight(1f))
                        TextButton(onClick = { onRemover(s) }) { Text("Remover") }
                    }
                }

                // Só mostra a parte de adicionar se ainda houver alguém disponível
                if (disponiveis.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Adicionar como", style = MaterialTheme.typography.titleSmall)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Papel.entries.forEach { p ->
                            FilterChip(
                                selected = papel == p,
                                onClick = { papel = p },
                                label = { Text(p.rotulo()) }
                            )
                        }
                    }
                    disponiveis.forEach { u ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(u.nomeCompleto, Modifier.weight(1f))
                            TextButton(onClick = { onAdicionar(u, papel) }) { Text("Adicionar") }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun PaginaItem(
    renderizador: RenderizadorPdf,
    indice: Int,
    documentoId: Long,
    campos: SnapshotStateList<CampoAssinatura>,
    signatarios: List<SignatarioDoDocumento>,
    selecionado: SignatarioDoDocumento?,
    repository: DocumentoRepository
) {
    val scope = rememberCoroutineScope()
    val bitmap by produceState<Bitmap?>(null, renderizador, indice) {
        value = renderizador.renderizarPagina(indice)
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.707f).background(Color.LightGray))
        return
    }

    var tamanhoPagina by remember { mutableStateOf(IntSize.Zero) }
    val atualSelecionado by rememberUpdatedState(selecionado)

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(bmp.width / bmp.height.toFloat())
            .onSizeChanged { tamanhoPagina = it }
            .pointerInput(indice) {
                detectTapGestures { offset ->
                    val signatario = atualSelecionado ?: return@detectTapGestures
                    val w = CampoAssinatura.LARGURA_PADRAO
                    val h = CampoAssinatura.ALTURA_PADRAO
                    val novo = CampoAssinatura(
                        documentoId = documentoId,
                        documentoSignatarioId = signatario.documentoSignatarioId,
                        pagina = indice,
                        posicaoX = (offset.x / size.width).coerceIn(0f, 1f - w),
                        posicaoY = (offset.y / size.height).coerceIn(0f, 1f - h),
                        largura = w,
                        altura = h
                    )
                    // Salva no banco na hora e coloca na tela já com o id real
                    scope.launch { campos += repository.adicionarCampo(novo) }
                }
            }
    ) {
        Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())

        campos.filter { it.pagina == indice }.forEach { campo ->
            key(campo.id) {
                val posicao = signatarios.indexOfFirst { it.documentoSignatarioId == campo.documentoSignatarioId }
                CampoBox(
                    campo = campo,
                    nome = signatarios.getOrNull(posicao)?.nome ?: "",
                    cor = corDoIndice(posicao.coerceAtLeast(0)),
                    tamanhoPagina = tamanhoPagina,
                    onMover = { atualizado ->
                        // Mover só atualiza a tela; o banco é gravado no Continuar
                        val i = campos.indexOfFirst { it.id == atualizado.id }
                        if (i >= 0) campos[i] = atualizado
                    },
                    onRemover = {
                        // Apagar remove do banco na hora
                        scope.launch {
                            repository.removerCampo(campo)
                            campos.removeAll { it.id == campo.id }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CampoBox(
    campo: CampoAssinatura,
    nome: String,
    cor: Color,
    tamanhoPagina: IntSize,
    onMover: (CampoAssinatura) -> Unit,
    onRemover: () -> Unit
) {
    if (tamanhoPagina == IntSize.Zero) return
    val density = LocalDensity.current
    val atualCampo by rememberUpdatedState(campo)
    val atualOnMover by rememberUpdatedState(onMover)

    Box(
        Modifier
            .offset {
                IntOffset(
                    (campo.posicaoX * tamanhoPagina.width).roundToInt(),
                    (campo.posicaoY * tamanhoPagina.height).roundToInt()
                )
            }
            .size(
                with(density) { (campo.largura * tamanhoPagina.width).toDp() },
                with(density) { (campo.altura * tamanhoPagina.height).toDp() }
            )
            .background(cor.copy(alpha = 0.15f))
            .border(2.dp, cor)
            // O toque rápido não abre mais o quadro de assinatura; só o toque longo apaga
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onRemover() })
            }
            .pointerInput(tamanhoPagina) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val f = atualCampo
                    val novaL = (f.largura * zoom).coerceIn(0.05f, 1f)
                    val novaA = (f.altura * zoom).coerceIn(0.03f, 1f)
                    val cx = f.posicaoX + f.largura / 2 + pan.x / tamanhoPagina.width
                    val cy = f.posicaoY + f.altura / 2 + pan.y / tamanhoPagina.height
                    atualOnMover(
                        f.copy(
                            posicaoX = (cx - novaL / 2).coerceIn(0f, 1f - novaL),
                            posicaoY = (cy - novaA / 2).coerceIn(0f, 1f - novaA),
                            largura = novaL,
                            altura = novaA
                        )
                    )
                }
            }
    ) {
        Text(nome, Modifier.align(Alignment.Center), color = cor, fontSize = 10.sp)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .background(cor)
                .pointerInput(tamanhoPagina) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val f = atualCampo
                        atualOnMover(
                            f.copy(
                                largura = (f.largura + drag.x / tamanhoPagina.width).coerceIn(0.05f, 1f - f.posicaoX),
                                altura = (f.altura + drag.y / tamanhoPagina.height).coerceIn(0.03f, 1f - f.posicaoY)
                            )
                        )
                    }
                }
        )
    }
}