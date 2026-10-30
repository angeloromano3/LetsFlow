package br.com.fiap.letsflow.ui.assinatura

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura
import br.com.fiap.letsflow.data.local.storage.RenderizadorPdf
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import kotlin.math.roundToInt

// Tela de leitura: mostra o documento com as assinaturas, sem permitir edição
@Composable
fun VisualizarDocumentoScreen(
    documentoId: Long, usuarioId: Long, onVoltar: () -> Unit
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

        // Primeiro nome de quem ainda falta assinar, pra montar a frase de situação
        val outrosPendentes = repository.listarSignatarios(documentoId)
            .filter { !it.assinado && it.documentoSignatarioId != vinculo.id }
            .map { it.nome.substringBefore(" ") }
        situacao = when {
            !vinculo.assinado -> "Você ainda não assinou este documento."
            outrosPendentes.size == 1 -> "Sua assinatura foi registrada. ${outrosPendentes[0]} ainda está pendente."
            outrosPendentes.size > 1 -> "Sua assinatura foi registrada. ${
                outrosPendentes.joinToString(
                    " e "
                )
            } ainda estão pendentes."

            else -> "Documento já assinado. Visualização sem edição."
        }

        // Concluído abre o PDF final; senão, abre o original e desenha as assinaturas por cima
        val pdfFinal = documento.caminhoPdfFinal
        usandoPdfFinal = pdfFinal != null
        renderizador = RenderizadorPdf(pdfFinal ?: documento.caminhoPdf)
        campos.addAll(repository.listarCamposDoSignatario(vinculo.id))
        paginaAtual =
            campos.minOfOrNull { it.pagina } ?: 0 // Começa na primeira página com assinatura
    }

    // Fecha o PDF ao sair da tela, pra liberar memória
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
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        cores.divisao,
                        RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                PaginaSomenteLeitura(
                    renderizador = atual, indice = paginaAtual,
                    // O PDF final já tem as assinaturas impressas, então não desenha de novo
                    campos = if (usandoPdfFinal) emptyList() else campos.filter { it.pagina == paginaAtual })
            }
            Spacer(Modifier.height(12.dp))

            // Setas pra trocar de página
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { paginaAtual-- }, enabled = paginaAtual > 0) {
                    Text("‹", fontSize = 20.sp)
                }
                Text(
                    "Página ${paginaAtual + 1} de ${atual.totalPaginas}",
                    style = tipografia.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = cores.texto
                )
                TextButton(
                    onClick = { paginaAtual++ }, enabled = paginaAtual < atual.totalPaginas - 1
                ) {
                    Text("›", fontSize = 20.sp)
                }
            }
        }
    }
}

// Desenha uma página do PDF com as assinaturas da pessoa por cima
@Composable
private fun PaginaSomenteLeitura(
    renderizador: RenderizadorPdf, indice: Int, campos: List<CampoAssinatura>
) {
    // Gera a imagem da página em segundo plano, sem travar a tela
    val bitmap by produceState<Bitmap?>(null, renderizador, indice) {
        value = renderizador.renderizarPagina(indice)
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(
            Modifier
                .aspectRatio(0.707f)
                .background(Color.White)
        ) // Página em branco enquanto carrega
        return
    }

    // Tamanho real da página na tela, usado pra converter as posições relativas
    var tamanhoPagina by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .aspectRatio(bmp.width / bmp.height.toFloat())
            .background(Color.White) // Página sempre branca, como papel, mesmo no modo escuro
            .onSizeChanged { tamanhoPagina = it }) {
        Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (tamanhoPagina != IntSize.Zero) {
            campos.forEach { campo -> key(campo.id) { AssinaturaNoCampo(campo, tamanhoPagina) } }
        }
    }
}

// Coloca a imagem da assinatura na posição e no tamanho do campo
@Composable
private fun AssinaturaNoCampo(campo: CampoAssinatura, tamanhoPagina: IntSize) {
    val caminho = campo.caminhoAssinatura ?: return
    val imagem = remember(caminho) { BitmapFactory.decodeFile(caminho)?.asImageBitmap() } ?: return
    val density = LocalDensity.current

    Image(
        imagem,
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
                with(density) { (campo.altura * tamanhoPagina.height).toDp() })
    )
}