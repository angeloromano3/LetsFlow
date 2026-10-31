package br.com.fiap.letsflow.ui.assinatura

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.data.local.storage.RenderizadorPdf
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.ui.componentes.ControlesZoom
import br.com.fiap.letsflow.ui.componentes.ZOOM_MINIMO
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import kotlin.math.roundToInt

// Tela de leitura: mostra o documento com as assinaturas, sem permitir edição
@Composable
fun VisualizarDocumentoScreen(
    documentoId: Long,
    usuarioId: Long,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember {
        val db = AppDatabase.getInstance(context)
        DocumentoRepository(
            db.documentoDao(),
            db.documentoSignatarioDao(),
            db.campoAssinaturaDao(),
            db.eventoDao()
        )
    }
    val cores = LetsFlowTheme.cores // Usa paleta clara ou escura
    val tipografia = MaterialTheme.typography // Estilos do Figma

    // Estados da tela: quando um deles muda, a tela se redesenha sozinha
    var titulo by remember { mutableStateOf("") }
    var situacao by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var renderizador by remember { mutableStateOf<RenderizadorPdf?>(null) } // Transforma páginas do PDF em imagem
    var usandoPdfFinal by remember { mutableStateOf(false) }
    val campos = remember { mutableStateListOf<CampoAssinatura>() } // Campos da pessoa logada
    var paginaAtual by remember { mutableIntStateOf(0) }
    // Zoom atual da página: 1 = largura da tela, 3 = três vezes maior
    var zoom by remember { mutableFloatStateOf(ZOOM_MINIMO) }

    // Ao abrir a tela, carrega o documento e confere se a pessoa pode vê-lo
    LaunchedEffect(documentoId, usuarioId) {
        val documento = repository.buscarPorId(documentoId)
        if (documento == null) {
            erro = "Documento não encontrado"
            return@LaunchedEffect
        }
        // Regra do Challenge: só quem é signatário acessa o documento
        val vinculo = repository.buscarVinculo(documentoId, usuarioId)
        if (vinculo == null) {
            erro = "Você não é signatário deste documento"
            return@LaunchedEffect
        }
        titulo = documento.titulo

        // A signatária só vê a própria situação; quem falta assinar é informação do Admin
        situacao = when {
            !vinculo.assinado -> "Você ainda não assinou este documento."
            documento.status == StatusDocumento.CONCLUIDO -> "Documento concluído. Todas as assinaturas foram registradas."
            else -> "Sua assinatura foi registrada. O documento será concluído quando todos assinarem."
        }

        // Concluído abre o PDF final; senão, abre o original e desenha as assinaturas por cima
        val pdfFinal = documento.caminhoPdfFinal
        usandoPdfFinal = pdfFinal != null
        renderizador = RenderizadorPdf(pdfFinal ?: documento.caminhoPdf)
        campos.addAll(repository.listarCamposDoSignatario(vinculo.id))
        paginaAtual = campos.minOfOrNull { it.pagina } ?: 0 // Começa na primeira página com assinatura
    }

    // Fecha o PDF ao sair da tela, para liberar memória
    val atual = renderizador
    DisposableEffect(atual) {
        onDispose { atual?.close() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Cabeçalho com seta de voltar e título
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←",
                fontSize = 22.sp,
                color = cores.texto,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onVoltar)
                    .padding(8.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Visualizar documento", style = tipografia.headlineMedium, color = cores.texto)
        }
        Spacer(Modifier.height(8.dp))
        Text(titulo, style = tipografia.bodyMedium, color = cores.textoSecundario)
        Spacer(Modifier.height(16.dp))
        Text("Modo somente leitura", style = tipografia.titleMedium, color = cores.acao)
        Text(
            "Este documento não pode ser alterado nesta visualização.",
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )
        Spacer(Modifier.height(16.dp))
        Text(
            situacao,
            style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = cores.texto
        )
        erro?.let {
            Text(
                it,
                color = cores.erro,
                style = tipografia.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        // Página e navegação só aparecem depois que o PDF carregou
        if (atual != null) {
            // Área da página com os botões de zoom flutuando por cima
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(cores.divisao, RoundedCornerShape(12.dp))
            ) {
                // Largura da página sem zoom: a área menos as margens
                val larguraBase = maxWidth - 16.dp

                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()) // Com zoom, dá para arrastar para cima e para baixo
                        .horizontalScroll(rememberScrollState()) // E também para os lados
                        .padding(8.dp)
                ) {
                    PaginaSomenteLeitura(
                        renderizador = atual,
                        indice = paginaAtual,
                        // O PDF final já tem as assinaturas impressas, então não desenha de novo
                        campos = if (usandoPdfFinal) emptyList() else campos.filter { it.pagina == paginaAtual },
                        largura = larguraBase * zoom // Página cresce de verdade com o zoom
                    )
                }

                // Botões flutuantes no canto inferior direito
                ControlesZoom(
                    zoom = zoom,
                    onZoomChange = { zoom = it },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            } // Fim da área da página: o que vem abaixo fica fora dela

            Spacer(Modifier.height(12.dp))

            // Setas para trocar de página
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center, // Centraliza "‹ Página X de Y ›"
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { paginaAtual-- }, enabled = paginaAtual > 0) {
                    Text("‹", fontSize = 20.sp)
                }
                Text(
                    "Página ${paginaAtual + 1} de ${atual.totalPaginas}",
                    style = tipografia.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = cores.texto
                )
                TextButton(onClick = { paginaAtual++ }, enabled = paginaAtual < atual.totalPaginas - 1) {
                    Text("›", fontSize = 20.sp)
                }
            }
        }
    }
}

// Desenha uma página do PDF com as assinaturas da pessoa por cima
@Composable
private fun PaginaSomenteLeitura(
    renderizador: RenderizadorPdf,
    indice: Int,
    campos: List<CampoAssinatura>,
    largura: Dp // Largura da página já com o zoom aplicado
) {
    // Gera a imagem da página em segundo plano, sem travar a tela
    val imagemCarregada by produceState<Bitmap?>(null, renderizador, indice) {
        value = renderizador.renderizarPagina(indice)
    }
    // Cópia fixa do estado, para o Kotlin deixar usar sem checar null de novo
    val imagemPagina = imagemCarregada
    if (imagemPagina == null) {
        // Página em branco enquanto carrega
        Box(
            Modifier
                .width(largura)
                .aspectRatio(0.707f)
                .background(Color.White)
        )
        return
    }

    // Tamanho real da página na tela, usado para converter as posições relativas
    var tamanhoPagina by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .width(largura) // Usa a largura com zoom
            .aspectRatio(imagemPagina.width / imagemPagina.height.toFloat())
            .background(Color.White) // Página sempre branca, como papel, mesmo no modo escuro
            .onSizeChanged { tamanhoPagina = it }
    ) {
        Image(imagemPagina.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (tamanhoPagina != IntSize.Zero) {
            campos.forEach { campo -> key(campo.id) { AssinaturaNoCampo(campo, tamanhoPagina) } }
        }
    }
}

// Coloca a imagem da assinatura na posição e no tamanho do campo
@Composable
private fun AssinaturaNoCampo(campo: CampoAssinatura, tamanhoPagina: IntSize) {
    val caminho = campo.caminhoAssinatura ?: return
    // Imagem da assinatura lida do arquivo PNG
    val imagemAssinatura = remember(caminho) { BitmapFactory.decodeFile(caminho)?.asImageBitmap() } ?: return
    val density = LocalDensity.current

    Image(
        imagemAssinatura,
        contentDescription = "Assinatura",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            // Posição relativa (0 a 1) vezes o tamanho da página = posição em pixels
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
    )
}