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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.componentes.ControlesZoom
import br.com.fiap.letsflow.ui.componentes.ZOOM_MINIMO
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import br.com.fiap.letsflow.ui.theme.corDoSignatario
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Nome do papel para mostrar na tela (a NovoDocumentoScreen também usa)
fun Papel.rotulo() = when (this) {
    Papel.CONTRATANTE -> "Contratante"
    Papel.CONTRATADO -> "Contratado"
    Papel.TESTEMUNHA -> "Testemunha"
}

// Iniciais do nome para o círculo da pessoa (ex.: "Maria Souza" -> "MS"); sem private: a NovoDocumentoScreen também usa
fun iniciais(nome: String): String {
    val partes = nome.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes[0].take(1).uppercase()
        else -> (partes.first().take(1) + partes.last().take(1)).uppercase()
    }
}

@Composable
fun PosicionamentoScreen(
    documentoId: Long,          // Vem da tela de novo documento
    onContinuar: () -> Unit     // Chamado depois que as posições são salvas
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cores = LetsFlowTheme.cores // Paleta clara ou escura do tema
    val tipografia = MaterialTheme.typography // Estilos do Figma

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
    // Zoom atual do PDF: 1 = largura da tela, 3 = três vezes maior
    var zoom by remember { mutableFloatStateOf(ZOOM_MINIMO) }

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

    // Libera o botão só quando cada pessoa tiver pelo menos um campo
    val todosComCampo = signatarios.isNotEmpty() && signatarios.all { signatario ->
        campos.any { it.documentoSignatarioId == signatario.documentoSignatarioId }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(cores.fundo) // Fundo do tema, acompanha o modo escuro
            .safeDrawingPadding()
    ) {
        // Cabeçalho compacto: título, etapa e atalho pra editar pessoas
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Posicionar assinaturas",
                    style = tipografia.headlineMedium,
                    color = cores.texto,
                    modifier = Modifier.weight(1f)
                )
                // Abre a janela de edição, mostrando quantos signatários há
                TextButton(onClick = { mostrarSignatarios = true }) {
                    Text("Pessoas (${signatarios.size})", color = cores.acao)
                }
            }
            Text("Etapa 2 de 3", style = tipografia.bodySmall, color = cores.textoSecundario)
            Spacer(Modifier.height(8.dp))
            // Instrução curta no topo
            Text(
                "Escolha a pessoa e toque na página para criar o campo. Arraste para mover e segure para apagar.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )
        }

        // Fileira única de nomes que rola para o lado; não cresce para baixo com mais pessoas
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            signatarios.forEachIndexed { indice, signatario ->
                val cor = corDoSignatario(indice) // Cor da pessoa vem do tema
                val ativo = signatario.documentoSignatarioId == selecionado?.documentoSignatarioId
                // Quantos campos essa pessoa já tem
                val quantidadeCampos = campos.count { it.documentoSignatarioId == signatario.documentoSignatarioId }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(if (ativo) cor else Color.Transparent, RoundedCornerShape(50))
                        .border(2.dp, cor, RoundedCornerShape(50))
                        .clickable { selecionado = signatario }
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    // Círculo com as iniciais, na cor da pessoa
                    Box(
                        Modifier
                            .size(26.dp)
                            .background(if (ativo) Color.White else cor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            iniciais(signatario.nome),
                            color = if (ativo) cor else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    // Primeiro nome e contador de campos, numa linha só
                    Text(
                        "${signatario.nome.substringBefore(" ")} · $quantidadeCampos",
                        color = if (ativo) Color.White else cor,
                        style = tipografia.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        erro?.let {
            Text(
                it,
                color = cores.erro,
                style = tipografia.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (atual != null) {
            // Área do PDF com os botões de zoom flutuando por cima
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(cores.divisao) // "Mesa" cinza atrás das páginas
            ) {
                // Largura da página sem zoom: a tela menos as margens
                val larguraBase = maxWidth - 24.dp

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()), // Arrasta pro lado quando a página passa da tela
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(atual.totalPaginas) { indice ->
                        PaginaItem(
                            renderizador = atual,
                            indice = indice,
                            documentoId = documentoId,
                            campos = campos,
                            signatarios = signatarios,
                            selecionado = selecionado,
                            repository = repository,
                            largura = larguraBase * zoom // Página cresce de verdade com o zoom
                        )
                    }
                }

                // Botões flutuantes no canto inferior direito, acima do "Revisar documento"
                ControlesZoom(
                    zoom = zoom,
                    onZoomChange = { zoom = it },
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // Canto inferior direito
                        .padding(16.dp)
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Substitui o "Logar campos": salva todas as posições finais de uma vez
        BotaoPrimario(
            texto = "Revisar documento →",
            onClick = {
                scope.launch {
                    val semCampo = signatarios.filter { signatario ->
                        campos.none { it.documentoSignatarioId == signatario.documentoSignatarioId }
                    }
                    if (semCampo.isNotEmpty()) {
                        erro = "Falta campo para: ${semCampo.joinToString { it.nome }}"
                        return@launch
                    }
                    repository.salvarPosicoes(campos.toList())
                    onContinuar()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            habilitado = todosComCampo // Cinza enquanto alguém estiver sem campo
        )
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
                signatarios.forEach { signatario ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${signatario.nome} (${signatario.papel.rotulo()})", Modifier.weight(1f))
                        TextButton(onClick = { onRemover(signatario) }) { Text("Remover") }
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
                        Papel.entries.forEach { opcao ->
                            FilterChip(
                                selected = papel == opcao,
                                onClick = { papel = opcao },
                                label = { Text(opcao.rotulo(), maxLines = 1) } // Não quebra "Testemunha"
                            )
                        }
                    }
                    disponiveis.forEach { usuario ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(usuario.nomeCompleto, Modifier.weight(1f))
                            TextButton(onClick = { onAdicionar(usuario, papel) }) { Text("Adicionar") }
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
    repository: DocumentoRepository,
    largura: Dp // Largura da página já com o zoom aplicado
) {
    val scope = rememberCoroutineScope()
    // Estado que recebe a página renderizada em segundo plano
    val imagemCarregada by produceState<Bitmap?>(null, renderizador, indice) {
        value = renderizador.renderizarPagina(indice)
    }
    // Cópia fixa do estado, pra o Kotlin deixar usar sem checar null de novo
    val imagemPagina = imagemCarregada
    if (imagemPagina == null) {
        // Página branca enquanto carrega, como papel
        Box(Modifier.width(largura).aspectRatio(0.707f).background(Color.White))
        return
    }

    var tamanhoPagina by remember { mutableStateOf(IntSize.Zero) }
    val atualSelecionado by rememberUpdatedState(selecionado)

    Box(
        Modifier
            .width(largura) // Usa a largura com zoom em vez de ocupar só a tela
            .aspectRatio(imagemPagina.width / imagemPagina.height.toFloat())
            .shadow(4.dp) // Sombrinha da folha sobre a mesa
            .background(Color.White) // Página sempre branca, mesmo no modo escuro
            .onSizeChanged { tamanhoPagina = it }
            .pointerInput(indice) {
                detectTapGestures { toque ->
                    val signatario = atualSelecionado ?: return@detectTapGestures
                    val larguraPadrao = CampoAssinatura.LARGURA_PADRAO
                    val alturaPadrao = CampoAssinatura.ALTURA_PADRAO
                    val novo = CampoAssinatura(
                        documentoId = documentoId,
                        documentoSignatarioId = signatario.documentoSignatarioId,
                        pagina = indice,
                        posicaoX = (toque.x / size.width).coerceIn(0f, 1f - larguraPadrao),
                        posicaoY = (toque.y / size.height).coerceIn(0f, 1f - alturaPadrao),
                        largura = larguraPadrao,
                        altura = alturaPadrao
                    )
                    // Salva no banco na hora e coloca na tela já com o id real
                    scope.launch { campos += repository.adicionarCampo(novo) }
                }
            }
    ) {
        Image(imagemPagina.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())

        campos.filter { it.pagina == indice }.forEach { campo ->
            key(campo.id) {
                val posicao = signatarios.indexOfFirst { it.documentoSignatarioId == campo.documentoSignatarioId }
                CampoBox(
                    campo = campo,
                    nome = signatarios.getOrNull(posicao)?.nome ?: "",
                    cor = corDoSignatario(posicao.coerceAtLeast(0)), // Cor da pessoa vem do tema
                    tamanhoPagina = tamanhoPagina,
                    onMover = { atualizado ->
                        // Mover só atualiza a tela; o banco é gravado no Continuar
                        val posicaoNaLista = campos.indexOfFirst { it.id == atualizado.id }
                        if (posicaoNaLista >= 0) campos[posicaoNaLista] = atualizado
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
                detectTransformGestures { _, movimento, escala, _ ->
                    // O campo antes do gesto, o novo tamanho e o centro do campo
                    val campoAntes = atualCampo
                    val novaLargura = (campoAntes.largura * escala).coerceIn(0.05f, 1f)
                    val novaAltura = (campoAntes.altura * escala).coerceIn(0.03f, 1f)
                    val centroX = campoAntes.posicaoX + campoAntes.largura / 2 + movimento.x / tamanhoPagina.width
                    val centroY = campoAntes.posicaoY + campoAntes.altura / 2 + movimento.y / tamanhoPagina.height
                    atualOnMover(
                        campoAntes.copy(
                            posicaoX = (centroX - novaLargura / 2).coerceIn(0f, 1f - novaLargura),
                            posicaoY = (centroY - novaAltura / 2).coerceIn(0f, 1f - novaAltura),
                            largura = novaLargura,
                            altura = novaAltura
                        )
                    )
                }
            }
    ) {
        Text(
            nome,
            Modifier.align(Alignment.Center),
            color = cor,
            fontSize = 10.sp,
            maxLines = 1, // Nome numa linha só dentro do campo
            overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .background(cor)
                .pointerInput(tamanhoPagina) {
                    detectDragGestures { change, arraste ->
                        change.consume()
                        val campoAntes = atualCampo // Campo antes de arrastar o cantinho
                        atualOnMover(
                            campoAntes.copy(
                                largura = (campoAntes.largura + arraste.x / tamanhoPagina.width)
                                    .coerceIn(0.05f, 1f - campoAntes.posicaoX),
                                altura = (campoAntes.altura + arraste.y / tamanhoPagina.height)
                                    .coerceIn(0.03f, 1f - campoAntes.posicaoY)
                            )
                        )
                    }
                }
        )
    }
}