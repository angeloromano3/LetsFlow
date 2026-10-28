package br.com.fiap.letsflow.ui.assinatura

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import br.com.fiap.letsflow.data.local.storage.ArquivoAssinatura
import br.com.fiap.letsflow.data.local.storage.RenderizadorPdf
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val corCampo = Color(0xFF1E88E5)

@Composable
fun AssinaturaScreen(
    documentoId: Long,
    usuarioId: Long,          // Signatário logado
    onConcluido: () -> Unit   // Volta pra lista de documentos
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

    // Carrega o documento e só os campos do signatário logado
    LaunchedEffect(documentoId, usuarioId) {
        val documento = repository.buscarPorId(documentoId)
        if (documento == null) {
            erro = "Documento não encontrado"
            return@LaunchedEffect
        }
        // Garante que um usuário não abre documento de outro
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
                scope.launch {
                    val id = documentoSignatarioId ?: return@launch
                    // Gravar arquivo é trabalho pesado: vai pra uma thread de fundo
                    val caminho = withContext(Dispatchers.IO) {
                        ArquivoAssinatura.salvar(context, tracos, "doc_${documentoId}_sig_${id}.png")
                    }
                    repository.assinar(documentoId, usuarioId, caminho)
                        .onSuccess {
                            jaAssinou = true
                            // Recarrega os campos, agora com a imagem da assinatura
                            campos.clear()
                            campos.addAll(repository.listarCamposDoSignatario(id))
                        }
                        .onFailure { erro = it.message }
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Text(titulo, Modifier.padding(8.dp), style = MaterialTheme.typography.titleLarge)
        Text(
            if (jaAssinou) "Você já assinou este documento." else "Confira o documento e toque em Assinar.",
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
                    PaginaAssinatura(atual, indice, campos.filter { it.pagina == indice })
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        Button(
            onClick = { if (jaAssinou) onConcluido() else mostrarQuadro = true },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text(if (jaAssinou) "Voltar" else "Assinar")
        }
    }
}

// Página só para leitura: aqui não dá pra mover nem apagar campos
@Composable
private fun PaginaAssinatura(
    renderizador: RenderizadorPdf,
    indice: Int,
    campos: List<CampoAssinatura>
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
            campos.forEach { campo -> key(campo.id) { CampoParaAssinar(campo, tamanhoPagina) } }
        }
    }
}

@Composable
private fun CampoParaAssinar(campo: CampoAssinatura, tamanhoPagina: IntSize) {
    val density = LocalDensity.current
    val caminho = campo.caminhoAssinatura
    // Só lê o PNG do disco quando o caminho muda
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
                // Campo pendente fica destacado; campo assinado mostra só a assinatura
                if (imagem == null) Modifier.background(corCampo.copy(alpha = 0.15f)).border(2.dp, corCampo)
                else Modifier
            )
    ) {
        if (imagem != null) {
            Image(imagem, contentDescription = "Assinatura", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Text("Assine aqui", Modifier.align(Alignment.Center), color = corCampo, fontSize = 10.sp)
        }
    }
}