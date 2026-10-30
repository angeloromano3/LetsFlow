package br.com.fiap.letsflow.ui.assinatura

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.componentes.BotaoSecundario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import br.com.fiap.letsflow.ui.theme.PaletaClara
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

// Os campos ficam em cima do PDF, que é sempre branco: por isso usam a cor da paleta clara, mesmo no modo escuro
private val corCampo = PaletaClara.acao

// Tela "Visualizar e assinar": a pessoa assina uma vez, confere no PDF e confirma
@Composable
fun AssinaturaScreen(
    documentoId: Long,
    usuarioId: Long,
    onConcluido: () -> Unit,
    onVoltar: () -> Unit // Sai da tela sem assinar
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
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    var renderizador by remember { mutableStateOf<RenderizadorPdf?>(null) }
    var titulo by remember { mutableStateOf("") }
    var primeiroNome by remember { mutableStateOf("") } // Aparece no rótulo dos campos
    var documentoSignatarioId by remember { mutableStateOf<Long?>(null) }
    var jaAssinou by remember { mutableStateOf(false) }
    var mostrarPainel by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val campos = remember { mutableStateListOf<CampoAssinatura>() }
    // Assinatura desenhada mas ainda não confirmada; null quando não há prévia
    var previa by remember { mutableStateOf<List<List<Offset>>?>(null) }
    // Evita apertar Confirmar duas vezes enquanto salva
    var salvando by remember { mutableStateOf(false) }
    // Fica true quando esta assinatura foi a última e o PDF final foi gerado
    var pdfFinalGerado by remember { mutableStateOf(false) }
    // true quando a assinatura foi salva agora, nesta tela: mostra o estado de sucesso
    var assinouAgora by remember { mutableStateOf(false) }
    // true quando a gravação falhou: mostra a caixa vermelha e mantém a prévia pra tentar de novo
    var falhaAoSalvar by remember { mutableStateOf(false) }

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
        primeiroNome = repository.listarSignatarios(documentoId)
            .find { it.documentoSignatarioId == vinculo.id }
            ?.nome?.substringBefore(" ").orEmpty()
        renderizador = RenderizadorPdf(documento.caminhoPdf)
        campos.addAll(repository.listarCamposDoSignatario(vinculo.id))
    }

    val atual = renderizador
    DisposableEffect(atual) {
        onDispose { atual?.close() }
    }

    // Só dá pra abrir o painel antes de assinar e fora da gravação
    val podeAssinar = !jaAssinou && !salvando && documentoSignatarioId != null
    val abrirPainel: () -> Unit = { if (podeAssinar) mostrarPainel = true }

    // Só aqui a assinatura vira PNG e vai pro banco
    fun confirmar(tracos: List<List<Offset>>) {
        val id = documentoSignatarioId ?: return
        scope.launch {
            salvando = true
            falhaAoSalvar = false
            // runCatching: qualquer erro, ao salvar o arquivo ou no banco, vira o estado de falha
            runCatching {
                val caminho = withContext(Dispatchers.IO) {
                    ArquivoAssinatura.salvar(context, tracos, "doc_${documentoId}_sig_${id}.png")
                }
                repository.assinar(documentoId, usuarioId, caminho).getOrThrow()
            }.onSuccess {
                jaAssinou = true
                assinouAgora = true
                previa = null
                campos.clear()
                campos.addAll(repository.listarCamposDoSignatario(id))
                // Se foi a última assinatura, gera o PDF final com todas as assinaturas
                if (repository.buscarPorId(documentoId)?.status == StatusDocumento.CONCLUIDO) {
                    repository.gerarPdfFinal(documentoId, context.filesDir)
                        .onSuccess { pdfFinalGerado = true }
                        .onFailure { erro = "Assinatura salva, mas houve um erro ao gerar o PDF final." }
                }
            }.onFailure {
                falhaAoSalvar = true
            }
            salvando = false
        }
    }

    if (mostrarPainel) {
        PainelAssinatura(
            onCancelar = { mostrarPainel = false },
            onAplicar = { tracos ->
                mostrarPainel = false
                // Não salva ainda: só mostra a prévia nos campos
                previa = tracos
                falhaAoSalvar = false
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
    ) {
        // Cabeçalho: seta, título, nome do documento, avisos e contador
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "←",
                    fontSize = 22.sp,
                    color = if (salvando) cores.textoDesabilitado else cores.texto, // Fica cinza enquanto salva
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !salvando) { // Bloqueada durante a gravação
                            previa = null // Descarta a prévia não confirmada
                            onVoltar()
                        }
                        .padding(8.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (assinouAgora) "Assinatura realizada" else "Visualizar e assinar",
                    style = tipografia.headlineMedium,
                    color = cores.texto
                )
            }
            Text(
                titulo,
                style = tipografia.bodyMedium,
                color = cores.textoSecundario,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.height(12.dp))

            // Caixas de aviso do Figma: sucesso, falha ou documento já assinado antes
            when {
                assinouAgora -> CaixaMensagem(
                    titulo = "Assinatura salva com sucesso",
                    texto = if (pdfFinalGerado) "Todos assinaram. O documento foi concluído."
                    else "Sua participação foi concluída. O documento seguirá pendente até as demais pessoas assinarem.",
                    cor = cores.acao,
                    fundo = cores.destaque
                )
                falhaAoSalvar -> CaixaMensagem(
                    titulo = "Não foi possível salvar",
                    texto = "Sua assinatura ainda não foi confirmada. Tente novamente.",
                    cor = cores.erro,
                    fundo = cores.fundoErro
                )
                jaAssinou -> CaixaMensagem(
                    titulo = "Documento já assinado",
                    texto = "Você já assinou este documento.",
                    cor = cores.acao,
                    fundo = cores.destaque
                )
            }

            // Contador: quantos dos campos designados pra esta pessoa já estão assinados
            if (campos.isNotEmpty()) {
                val feitos = if (jaAssinou) campos.size else 0
                Text(
                    "Suas assinaturas · $feitos de ${campos.size}",
                    style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (jaAssinou) cores.concluido else cores.texto
                )
                if (!jaAssinou) {
                    Text(
                        if (previa != null) "Confira sua assinatura nos campos antes de confirmar."
                        else "Toque em Assinar ou em um campo destacado.",
                        style = tipografia.bodySmall,
                        color = cores.textoSecundario
                    )
                }
            }
            erro?.let {
                Text(it, color = cores.erro, style = tipografia.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (atual != null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(atual.totalPaginas) { indice ->
                    // Passa a prévia pra página desenhar nos campos
                    PaginaAssinatura(
                        renderizador = atual,
                        indice = indice,
                        campos = campos.filter { it.pagina == indice },
                        previa = previa,
                        rotulo = if (primeiroNome.isBlank()) "Toque para assinar" else "$primeiroNome · Toque para assinar",
                        clicavel = podeAssinar && previa == null,
                        onTocarCampo = abrirPainel
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Botões de ação um embaixo do outro, ocupando a largura toda, como no Figma
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tracosPrevia = previa
            when {
                jaAssinou -> BotaoPrimario(texto = "Voltar aos meus documentos", onClick = onConcluido)

                // Etapa de prévia (ou de falha): confirmar de vez ou refazer
                tracosPrevia != null -> {
                    BotaoPrimario(
                        texto = if (salvando) "Salvando..." else "Confirmar assinatura",
                        onClick = { confirmar(tracosPrevia) },
                        habilitado = !salvando
                    )
                    // Descarta a prévia e abre o painel de novo
                    BotaoSecundario(
                        texto = "Refazer",
                        onClick = {
                            previa = null
                            mostrarPainel = true
                        },
                        habilitado = !salvando
                    )
                }

                else -> BotaoPrimario(texto = "Assinar", onClick = abrirPainel, habilitado = podeAssinar)
            }
        }
    }
}

// Caixa de aviso do Figma: título colorido e texto explicativo
@Composable
private fun CaixaMensagem(titulo: String, texto: String, cor: Color, fundo: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(fundo, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = cor)
        Text(texto, style = MaterialTheme.typography.bodySmall, color = LetsFlowTheme.cores.textoSecundario)
    }
}

@Composable
private fun PaginaAssinatura(
    renderizador: RenderizadorPdf,
    indice: Int,
    campos: List<CampoAssinatura>,
    previa: List<List<Offset>>?, // Traços da prévia, se houver
    rotulo: String,
    clicavel: Boolean,
    onTocarCampo: () -> Unit
) {
    val bitmap by produceState<Bitmap?>(null, renderizador, indice) {
        value = renderizador.renderizarPagina(indice)
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.707f)
                .background(Color.White) // Página em branco enquanto carrega
        )
        return
    }

    var tamanhoPagina by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(bmp.width / bmp.height.toFloat())
            .background(Color.White) // Página sempre branca, como papel
            .border(1.dp, LetsFlowTheme.cores.divisao)
            .onSizeChanged { tamanhoPagina = it }
    ) {
        Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (tamanhoPagina != IntSize.Zero) {
            campos.forEach { campo ->
                key(campo.id) {
                    CampoParaAssinar(campo, tamanhoPagina, previa, rotulo, clicavel, onTocarCampo)
                }
            }
        }
    }
}

@Composable
private fun CampoParaAssinar(
    campo: CampoAssinatura,
    tamanhoPagina: IntSize,
    previa: List<List<Offset>>?, // Traços da prévia, se houver
    rotulo: String,
    clicavel: Boolean,
    onTocar: () -> Unit
) {
    val density = LocalDensity.current
    val caminho = campo.caminhoAssinatura
    val imagem = remember(caminho) {
        caminho?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    val formato = RoundedCornerShape(4.dp)

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
                when {
                    imagem != null -> Modifier // Assinado: só a assinatura, sem borda
                    previa != null -> Modifier.border(1.dp, corCampo.copy(alpha = 0.4f), formato) // Prévia: borda leve, ainda não confirmada
                    else -> Modifier // Pendente: destacado e tocável, como no Figma
                        .clip(formato)
                        .background(corCampo.copy(alpha = 0.12f))
                        .border(1.5.dp, corCampo, formato)
                        .clickable(enabled = clicavel, onClick = onTocar)
                }
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
            else -> Text(
                "✎ $rotulo",
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 4.dp),
                color = corCampo,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Campo pequeno corta o texto com "..." em vez de quebrar
            )
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