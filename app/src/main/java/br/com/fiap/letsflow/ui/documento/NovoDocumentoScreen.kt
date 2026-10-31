package br.com.fiap.letsflow.ui.documento

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.Papel
import br.com.fiap.letsflow.data.local.entity.Usuario
import br.com.fiap.letsflow.data.local.storage.ArquivoPdf
import br.com.fiap.letsflow.data.local.storage.RenderizadorPdf
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.data.repository.UsuarioRepository
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.componentes.BotaoSecundario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import br.com.fiap.letsflow.ui.theme.corDoSignatario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Tela onde o Admin informa o título, escolhe o PDF e os signatários com o papel de cada um
@Composable
fun NovoDocumentoScreen(
    adminId: Long,
    onSalvo: (documentoId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onCadastrarPessoa: () -> Unit = {} // Vai abrir o cadastro da Mariana; por enquanto não faz nada
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cores = LetsFlowTheme.cores // Paleta clara ou escura do tema
    val tipografia = MaterialTheme.typography // Estilos do Figma

    // Repositórios criados uma vez só; depois isso vai para um ViewModel
    val db = remember { AppDatabase.getInstance(context) }
    val usuarioRepository = remember { UsuarioRepository(db.usuarioDao()) }
    val documentoRepository = remember {
        DocumentoRepository(
            db.documentoDao(),
            db.documentoSignatarioDao(),
            db.campoAssinaturaDao(),
            db.eventoDao()
        )
    }
    val arquivoPdf = remember { ArquivoPdf(context) }

    var titulo by remember { mutableStateOf("") }
    var caminhoPdf by remember { mutableStateOf<String?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var nomeArquivo by remember { mutableStateOf("") } // Nome que aparece no card do PDF
    var totalPaginas by remember { mutableIntStateOf(0) }
    var pdfInvalido by remember { mutableStateOf(false) } // Mostra a caixa vermelha do Figma
    var carregandoPdf by remember { mutableStateOf(false) }
    var busca by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) } // Evita salvar duas vezes

    // Signatários marcados: id do usuário -> papel
    val selecionados = remember { mutableStateMapOf<Long, Papel>() }

    // A lista atualiza sozinha por causa do Flow
    val signatarios by usuarioRepository.listarSignatarios().collectAsState(initial = emptyList())

    // Abre o seletor de arquivos do Android, filtrando só PDF
    val escolherPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                carregandoPdf = true
                arquivoPdf.salvar(uri)
                    .mapCatching { caminho ->
                        // Testa o PDF logo ao escolher: se não abrir ou não tiver páginas, é inválido
                        val paginas = withContext(Dispatchers.IO) { contarPaginas(caminho) }
                        require(paginas > 0)
                        caminho to paginas
                    }
                    .onSuccess { (caminho, paginas) ->
                        caminhoPdf = caminho
                        totalPaginas = paginas
                        nomeArquivo = withContext(Dispatchers.IO) { nomeDoArquivo(context, uri) }
                        pdfInvalido = false
                        mensagem = null
                    }
                    .onFailure {
                        caminhoPdf = null
                        pdfInvalido = true
                    }
                carregandoPdf = false
            }
        }
    }

    // Busca por nome ou e-mail, sem diferenciar maiúscula de minúscula
    val textoBusca = busca.trim()
    val pessoasFiltradas = signatarios.filter {
        textoBusca.isEmpty() ||
                it.nomeCompleto.contains(textoBusca, ignoreCase = true) ||
                it.email.contains(textoBusca, ignoreCase = true)
    }

    // Continuar só libera com título, PDF válido e pelo menos uma pessoa
    val podeContinuar = titulo.isNotBlank() && caminhoPdf != null &&
            selecionados.isNotEmpty() && !salvando && !carregandoPdf

    Column(
        modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
    ) {
        // Tudo rola junto, para o teclado não esconder os campos
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabeçalho no mesmo padrão do "Etapa 2 de 3" do posicionamento
            item {
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text("Novo documento", style = tipografia.headlineMedium, color = cores.texto)
                    Text("Etapa 1 de 3", style = tipografia.bodyMedium, color = cores.textoSecundario)
                }
            }

            item {
                Column {
                    RotuloCampo("Título do documento *")
                    CampoTexto(
                        valor = titulo,
                        onValorChange = { titulo = it },
                        dica = "Ex: Contrato de prestação de serviços"
                    )
                }
            }

            item {
                Column(Modifier.padding(top = 8.dp)) {
                    RotuloCampo("Arquivo PDF *")
                    CartaoPdf(
                        caminhoPdf = caminhoPdf,
                        nomeArquivo = nomeArquivo,
                        totalPaginas = totalPaginas,
                        invalido = pdfInvalido,
                        carregando = carregandoPdf,
                        onEscolher = { escolherPdf.launch(arrayOf("application/pdf")) }
                    )
                }
            }

            // Título da seção com o contador de pessoas marcadas
            item {
                Column(Modifier.padding(top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Pessoas",
                            style = tipografia.titleLarge,
                            color = cores.texto,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (selecionados.size == 1) "1 selecionada" else "${selecionados.size} selecionadas",
                            style = tipografia.bodySmall,
                            color = cores.textoSecundario
                        )
                    }
                    Text(
                        "Marque quem vai assinar e escolha o papel de cada um.",
                        style = tipografia.bodySmall,
                        color = cores.textoSecundario,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    CampoTexto(
                        valor = busca,
                        onValorChange = { busca = it },
                        dica = "Buscar por nome ou e-mail"
                    )
                }
            }

            if (pessoasFiltradas.isEmpty()) {
                item {
                    Text(
                        "Nenhuma pessoa encontrada.",
                        style = tipografia.bodyMedium,
                        color = cores.textoSecundario,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            items(pessoasFiltradas, key = { it.id }) { usuario ->
                LinhaPessoa(
                    usuario = usuario,
                    indice = signatarios.indexOf(usuario), // Cor fixa pela posição na lista completa, não na filtrada
                    papel = selecionados[usuario.id],
                    // Marcar já coloca como contratante; desmarcar remove
                    onMarcar = { marcado ->
                        if (marcado) selecionados[usuario.id] = Papel.CONTRATANTE
                        else selecionados.remove(usuario.id)
                    },
                    onEscolherPapel = { opcao -> selecionados[usuario.id] = opcao }
                )
            }

            item {
                BotaoSecundario(
                    texto = "+ Cadastrar outra pessoa",
                    onClick = onCadastrarPessoa,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }

        // Rodapé fixo com o botão, fora da rolagem
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            mensagem?.let {
                Text(
                    it,
                    color = cores.erro,
                    style = tipografia.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            BotaoPrimario(
                texto = if (salvando) "Salvando..." else "Continuar",
                onClick = {
                    val caminho = caminhoPdf ?: return@BotaoPrimario
                    scope.launch {
                        salvando = true
                        documentoRepository.salvarRascunho(
                            titulo,
                            caminho,
                            adminId,
                            selecionados.toMap()
                        )
                            .onSuccess { onSalvo(it) }
                            .onFailure { mensagem = it.message }
                        salvando = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                habilitado = podeContinuar
            )
        }
    }
}

// Rótulo em cima do campo, como no Figma
@Composable
private fun RotuloCampo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = LetsFlowTheme.cores.texto,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

// Campo de texto com as cores do tema
@Composable
private fun CampoTexto(valor: String, onValorChange: (String) -> Unit, dica: String) {
    val cores = LetsFlowTheme.cores
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        placeholder = { Text(dica, color = cores.textoSecundario) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cores.superficie,
            unfocusedContainerColor = cores.superficie,
            focusedBorderColor = cores.acao,
            unfocusedBorderColor = cores.divisao,
            focusedTextColor = cores.texto,
            unfocusedTextColor = cores.texto,
            cursorColor = cores.acao
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// Card do PDF com os estados do Figma: não selecionado, carregando, válido e inválido
@Composable
private fun CartaoPdf(
    caminhoPdf: String?,
    nomeArquivo: String,
    totalPaginas: Int,
    invalido: Boolean,
    carregando: Boolean,
    onEscolher: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val formato = RoundedCornerShape(12.dp)

    when {
        carregando -> Box(
            Modifier
                .fillMaxWidth()
                .background(cores.superficie, formato)
                .border(1.dp, cores.divisao, formato)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Abrindo arquivo...", style = tipografia.bodyMedium, color = cores.textoSecundario)
        }

        // Válido: nome, páginas e opção de substituir
        caminhoPdf != null -> Row(
            Modifier
                .fillMaxWidth()
                .background(cores.superficie, formato)
                .border(1.dp, cores.divisao, formato)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(cores.destaque, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("PDF", color = cores.acao, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    nomeArquivo,
                    style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // Nome comprido corta com "..."
                )
                Text(
                    if (totalPaginas == 1) "1 página · Arquivo válido" else "$totalPaginas páginas · Arquivo válido",
                    style = tipografia.bodySmall,
                    color = cores.concluido
                )
            }
            Text(
                "Substituir",
                style = tipografia.labelLarge,
                color = cores.acao,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEscolher)
                    .padding(8.dp)
            )
        }

        // Não selecionado ou inválido: área tracejada para escolher o arquivo
        else -> Column {
            if (invalido) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(cores.fundoErro, formato)
                        .padding(12.dp)
                ) {
                    Text(
                        "Não foi possível abrir este arquivo",
                        style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = cores.erro
                    )
                    Text(
                        "Escolha outro PDF. O arquivo pode estar corrompido ou protegido.",
                        style = tipografia.bodySmall,
                        color = cores.textoSecundario
                    )
                }
            }
            val corTracejado = cores.acao
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(formato)
                    .background(cores.superficie)
                    .drawBehind {
                        // Borda tracejada desenhada à mão, porque o border() só faz linha contínua
                        drawRoundRect(
                            color = corTracejado,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                    }
                    .clickable(onClick = onEscolher)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (invalido) "Escolher outro PDF" else "Importar arquivo PDF",
                    style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.acao
                )
                Text(
                    "Toque para escolher um PDF do aparelho",
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
            }
        }
    }
}

// Uma pessoa da lista: toque marca ou desmarca, e os papéis aparecem quando marcada
@Composable
private fun LinhaPessoa(
    usuario: Usuario,
    indice: Int,
    papel: Papel?,
    onMarcar: (Boolean) -> Unit,
    onEscolherPapel: (Papel) -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val marcado = papel != null
    val formato = RoundedCornerShape(12.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(formato)
            .background(if (marcado) cores.destaque else cores.superficie)
            .border(
                if (marcado) 1.5.dp else 1.dp,
                if (marcado) cores.acao else cores.divisao,
                formato
            )
            .clickable { onMarcar(!marcado) } // A linha inteira é tocável, não só a caixinha
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Círculo com as iniciais, na mesma cor que a pessoa terá no posicionamento
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(corDoSignatario(indice)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    iniciais(usuario.nomeCompleto), // Mesma função da PosicionamentoScreen
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    usuario.nomeCompleto,
                    style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    usuario.email,
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Checkbox(
                checked = marcado,
                onCheckedChange = onMarcar,
                colors = CheckboxDefaults.colors(
                    checkedColor = cores.acao,
                    uncheckedColor = cores.textoSecundario,
                    checkmarkColor = cores.textoSobreAcao
                )
            )
        }

        // Os papéis só aparecem para quem está marcado
        if (papel != null) {
            Spacer(Modifier.height(10.dp))
            Text("Papel no documento", style = tipografia.bodySmall, color = cores.textoSecundario)
            Spacer(Modifier.height(6.dp))
            // Rola para o lado em vez de quebrar linha quando os chips não cabem
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Papel.entries.forEach { opcao ->
                    ChipPapel(
                        texto = opcao.rotulo(),
                        selecionado = papel == opcao,
                        onClick = { onEscolherPapel(opcao) }
                    )
                }
            }
        }
    }
}

// Chip de papel: preenchido quando escolhido, contorno quando não
@Composable
private fun ChipPapel(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    val cores = LetsFlowTheme.cores
    val formato = RoundedCornerShape(50)
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selecionado) cores.textoSobreAcao else cores.texto,
        maxLines = 1, // Nunca quebra o nome do papel em duas linhas
        modifier = Modifier
            .clip(formato)
            .background(if (selecionado) cores.acao else cores.superficie)
            .border(1.dp, if (selecionado) cores.acao else cores.divisao, formato)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

// Abre o PDF só para contar as páginas; o use fecha sozinho mesmo se der erro
private fun contarPaginas(caminho: String): Int =
    RenderizadorPdf(caminho).use { it.totalPaginas }

// Pega o nome original do arquivo escolhido, por exemplo "contrato.pdf"
private fun nomeDoArquivo(context: Context, uri: Uri): String =
    context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: "documento.pdf"