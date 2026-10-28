package br.com.fiap.letsflow.ui.assinatura

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.data.local.storage.ArquivoAssinatura
import br.com.fiap.letsflow.data.local.storage.RenderizadorPdf
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private val corCampo = Color(0xFF1E88E5)

@Composable
fun AssinaturaScreen(
    documentoId: Long,
    usuarioId: Long,
    onConcluido: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var titulo by remember { mutableStateOf("") }
    var documentoSignatarioId by remember { mutableStateOf<Long?>(null) }
    var jaAssinou by remember { mutableStateOf(false) }
    var mostrarQuadro by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val campos = remember { mutableStateListOf<CampoAssinatura>() }
    // Assinatura desenhada mas ainda não confirmada; null quando não há prévia
    var previa by remember { mutableStateOf<List<List<Offset>>?>(null) }
    // Evita apertar Confirmar duas vezes enquanto salva
    var salvando by remember { mutableStateOf(false) }
    // Fica true quando esta assinatura foi a última e o PDF final foi gerado
    var pdfFinalGerado by remember { mutableStateOf(false) }

    LaunchedEffect(documentoId, usuarioId) {
        val documento = repository.buscarPorId(documentoId)
        if (documento == null) {
            erro = "Documento não encontrado"
            return@LaunchedEffect
        }
        val vinculo = repository.buscarVinculo(documentoId, usuarioId)
        if (vinculo == null) {
            erro = "Você não é signatário deste documento"
            return@LaunchedEffect
        }
        titulo = documento.titulo
        documentoSignatarioId = vinculo.id
        jaAssinou = vinculo.assinado
        renderizador = RenderizadorPdf(documento.caminhoPdf)
        campos.addAll(repository.listarCamposDoSignatario(vinculo.id))
    }

    val atual = renderizador
    DisposableEffect(atual) {
        onDispose { atual?.close() }
    }

    if (mostrarQuadro) {
        QuadroAssinaturaDialog(
            onCancelar = { mostrarQuadro = false },
            onConfirmar = { tracos ->
                mostrarQuadro = false
                // Não salva ainda: só mostra a prévia nos campos
                previa = tracos
            }
        )
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Text(titulo, Modifier.padding(8.dp), style = MaterialTheme.typography.titleLarge)
        Text(
            when {
                // Mensagem quando esta foi a última assinatura
                pdfFinalGerado -> "Todos assinaram. O documento final foi gerado."
                jaAssinou -> "Você já assinou este documento."
                // Texto da etapa de prévia
                previa != null -> "Confira sua assinatura nos campos antes de confirmar."
                else -> "Confira o documento e toque em Assinar."
            },
            Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
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
                    // Passa a prévia pra página desenhar nos campos
                    PaginaAssinatura(atual, indice, campos.filter { it.pagina == indice }, previa)
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        val tracosPrevia = previa
        when {
            jaAssinou -> Button(
                onClick = onConcluido,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) { Text("Voltar") }

            // Etapa de prévia: refazer ou confirmar de vez
            tracosPrevia != null -> Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Descarta a prévia e abre o quadro de novo
                OutlinedButton(
                    onClick = {
                        previa = null
                        mostrarQuadro = true
                    },
                    enabled = !salvando,
                    modifier = Modifier.weight(1f)
                ) { Text("Refazer") }

                // Só aqui a assinatura vira PNG e é gravada no banco
                Button(
                    onClick = {
                        scope.launch {
                            val id = documentoSignatarioId ?: return@launch
                            salvando = true
                            val caminho = withContext(Dispatchers.IO) {
                                ArquivoAssinatura.salvar(context, tracosPrevia, "doc_${documentoId}_sig_${id}.png")
                            }
                            repository.assinar(documentoId, usuarioId, caminho)
                                .onSuccess {
                                    jaAssinou = true
                                    previa = null
                                    campos.clear()
                                    campos.addAll(repository.listarCamposDoSignatario(id))
                                    // Se foi a última assinatura, gera o PDF final com todas as assinaturas
                                    if (repository.buscarPorId(documentoId)?.status == StatusDocumento.CONCLUIDO) {
                                        repository.gerarPdfFinal(documentoId, context.filesDir)
                                            .onSuccess { pdfFinalGerado = true }
                                            .onFailure { erro = "Erro ao gerar o PDF final: ${it.message}" }
                                    }
                                }
                                .onFailure { erro = it.message }
                            salvando = false
                        }
                    },
                    enabled = !salvando,
                    modifier = Modifier.weight(1f)
                ) { Text(if (salvando) "Salvando..." else "Confirmar assinatura") } // Mostra que está processando
            }

            else -> Button(
                onClick = { mostrarQuadro = true },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) { Text("Assinar") }
        }
    }
}

@Composable
private fun PaginaAssinatura(
    renderizador: RenderizadorPdf,
    indice: Int,
    campos: List<CampoAssinatura>,
    previa: List<List<Offset>>? // Traços da prévia, se houver
) {
    val bitmap by produceState<Bitmap?>(null, renderizador, indice) {
        value = renderizador.renderizarPagina(indice)
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.707f).background(Color.LightGray))
        return
    }

    var tamanhoPagina by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(bmp.width / bmp.height.toFloat())
            .onSizeChanged { tamanhoPagina = it }
    ) {
        Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (tamanhoPagina != IntSize.Zero) {
            campos.forEach { campo -> key(campo.id) { CampoParaAssinar(campo, tamanhoPagina, previa) } }
        }
    }
}

@Composable
private fun CampoParaAssinar(
    campo: CampoAssinatura,
    tamanhoPagina: IntSize,
    previa: List<List<Offset>>? // Traços da prévia, se houver
) {
    val density = LocalDensity.current
    val caminho = campo.caminhoAssinatura
    val imagem = remember(caminho) { caminho?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }

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
            .then(
                if (imagem == null) Modifier.background(corCampo.copy(alpha = 0.15f)).border(2.dp, corCampo)
                else Modifier
            )
    ) {
        when {
            imagem != null -> Image(
                imagem,
                contentDescription = "Assinatura",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            // Desenha a prévia direto no campo, sem salvar arquivo
            previa != null -> Canvas(Modifier.fillMaxSize()) {
                previa.forEach { desenharTracoPrevia(it) }
            }
            else -> Text("Assine aqui", Modifier.align(Alignment.Center), color = corCampo, fontSize = 10.sp)
        }
    }
}

// Desenha a prévia na proporção 3:1, centralizada no campo, igual ao PNG final com ContentScale.Fit
private fun DrawScope.desenharTracoPrevia(pontos: List<Offset>) {
    if (pontos.size < 2) return
    val largura = min(size.width, size.height * 3f)
    val altura = largura / 3f
    val inicioX = (size.width - largura) / 2f
    val inicioY = (size.height - altura) / 2f
    val caminho = Path().apply {
        moveTo(inicioX + pontos[0].x * largura, inicioY + pontos[0].y * altura)
        pontos.drop(1).forEach { lineTo(inicioX + it.x * largura, inicioY + it.y * altura) }
    }
    drawPath(
        caminho,
        Color.Black,
        style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}