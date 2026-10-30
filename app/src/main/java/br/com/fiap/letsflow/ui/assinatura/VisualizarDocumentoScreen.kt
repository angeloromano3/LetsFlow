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
import br.com.fiap.letsflow.ui.theme.AzulLetsFlow
import br.com.fiap.letsflow.ui.theme.BordaCartao
import br.com.fiap.letsflow.ui.theme.FundoTela
import br.com.fiap.letsflow.ui.theme.TextoPrincipal
import br.com.fiap.letsflow.ui.theme.TextoSecundario
import kotlin.math.roundToInt

// Documento em modo somente leitura: o signatário confere a assinatura sem poder alterar nada
@Composable
fun VisualizarDocumentoScreen(
    documentoId: Long,
    usuarioId: Long,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember {
        val db = AppDatabase.getInstance(context)
        DocumentoRepository(db.documentoDao(), db.documentoSignatarioDao(), db.campoAssinaturaDao(), db.eventoDao())
    }

    var titulo by remember { mutableStateOf("") }
    var situacao by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var renderizador by remember { mutableStateOf<RenderizadorPdf?>(null) }
    // true quando mostramos o PDF final, que já traz todas as assinaturas desenhadas
    var usandoPdfFinal by remember { mutableStateOf(false) }
    val campos = remember { mutableStateListOf<CampoAssinatura>() }
    var paginaAtual by remember { mutableIntStateOf(0) }

    LaunchedEffect(documentoId, usuarioId) {
        val documento = repository.buscarPorId(documentoId)
        if (documento == null) {
            erro = "Documento não encontrado"
            return@LaunchedEffect
        }
        // Mesmo controle de acesso da assinatura: só quem é signatário abre
        val vinculo = repository.buscarVinculo(documentoId, usuarioId)
        if (vinculo == null) {
            erro = "Você não é signatário deste documento"
            return@LaunchedEffect
        }
        titulo = documento.titulo

        // Primeiro nome de quem ainda falta assinar (sem contar a própria pessoa)
        val outrosPendentes = repository.listarSignatarios(documentoId)
            .filter { !it.assinado && it.documentoSignatarioId != vinculo.id }
            .map { it.nome.substringBefore(" ") }
        situacao = when {
            !vinculo.assinado -> "Você ainda não assinou este documento."
            outrosPendentes.size == 1 -> "Sua assinatura foi registrada. ${outrosPendentes[0]} ainda está pendente."
            outrosPendentes.size > 1 ->
                "Sua assinatura foi registrada. ${outrosPendentes.joinToString(" e ")} ainda estão pendentes."
            else -> "Documento já assinado. Visualização sem edição."
        }

        // Se todos assinaram, mostra o PDF final; senão, o original com as assinaturas desta pessoa por cima
        val pdfFinal = documento.caminhoPdfFinal
        usandoPdfFinal = pdfFinal != null
        renderizador = RenderizadorPdf(pdfFinal ?: documento.caminhoPdf)
        campos.addAll(repository.listarCamposDoSignatario(vinculo.id))
        // Abre direto na página da primeira assinatura, como no protótipo
        paginaAtual = campos.minOfOrNull { it.pagina } ?: 0
    }

    val atual = renderizador
    DisposableEffect(atual) {
        onDispose { atual?.close() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(FundoTela)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←",
                fontSize = 22.sp,
                color = TextoPrincipal,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onVoltar).padding(8.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Visualizar documento", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextoPrincipal)
        }
        Spacer(Modifier.height(8.dp))
        Text(titulo, fontSize = 13.sp, color = TextoSecundario)
        Spacer(Modifier.height(16.dp))
        Text("Modo somente leitura", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AzulLetsFlow)
        Text("Este documento não pode ser alterado nesta visualização.", fontSize = 12.sp, color = TextoSecundario)
        Spacer(Modifier.height(16.dp))
        Text(situacao, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextoPrincipal)
        erro?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))

        if (atual != null) {
            // Moldura cinza em volta da página; weight faz a página caber na altura que sobra
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(BordaCartao, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                PaginaSomenteLeitura(
                    renderizador = atual,
                    indice = paginaAtual,
                    // No PDF final as assinaturas já estão desenhadas na página
                    campos = if (usandoPdfFinal) emptyList() else campos.filter { it.pagina == paginaAtual }
                )
            }
            Spacer(Modifier.height(12.dp))

            // Navegação "‹ Página X de Y ›"
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { paginaAtual-- }, enabled = paginaAtual > 0) {
                    Text("‹", fontSize = 20.sp)
                }
                Text(
                    "Página ${paginaAtual + 1} de ${atual.totalPaginas}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextoPrincipal
                )
                TextButton(onClick = { paginaAtual++ }, enabled = paginaAtual < atual.totalPaginas - 1) {
                    Text("›", fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun PaginaSomenteLeitura(
    renderizador: RenderizadorPdf,
    indice: Int,
    campos: List<CampoAssinatura>
) {
    val bitmap by produceState<Bitmap?>(null, renderizador, indice) {
        value = renderizador.renderizarPagina(indice)
    }
    val bmp = bitmap
    if (bmp == null) {
        Box(Modifier.aspectRatio(0.707f).background(Color.White))
        return
    }

    var tamanhoPagina by remember { mutableStateOf(IntSize.Zero) }

    // Sem fillMaxWidth: o aspectRatio escolhe o maior tamanho que cabe na moldura
    Box(
        Modifier
            .aspectRatio(bmp.width / bmp.height.toFloat())
            .background(Color.White)
            .onSizeChanged { tamanhoPagina = it }
    ) {
        Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (tamanhoPagina != IntSize.Zero) {
            campos.forEach { campo -> key(campo.id) { AssinaturaNoCampo(campo, tamanhoPagina) } }
        }
    }
}

// Só mostra a imagem da assinatura no lugar do campo, sem borda nem toque
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