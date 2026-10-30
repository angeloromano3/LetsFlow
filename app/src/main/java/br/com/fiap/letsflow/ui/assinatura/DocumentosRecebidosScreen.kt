package br.com.fiap.letsflow.ui.assinatura

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.model.DocumentoRecebido
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.data.repository.UsuarioRepository
import br.com.fiap.letsflow.ui.theme.AzulLetsFlow
import br.com.fiap.letsflow.ui.theme.BordaCartao
import br.com.fiap.letsflow.ui.theme.CorAssinado
import br.com.fiap.letsflow.ui.theme.CorPendente
import br.com.fiap.letsflow.ui.theme.FundoTela
import br.com.fiap.letsflow.ui.theme.TextoPrincipal
import br.com.fiap.letsflow.ui.theme.TextoSecundario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// "Meus documentos" do signatário, com abas Pendentes e Assinados, seguindo o protótipo do Figma
@Composable
fun DocumentosRecebidosScreen(
    usuarioId: Long,
    onAssinar: (documentoId: Long) -> Unit,    // Abre a tela de assinatura
    onVisualizar: (documentoId: Long) -> Unit, // Abre o documento em modo leitura
    onSair: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val documentoRepository = remember {
        DocumentoRepository(
            db.documentoDao(),
            db.documentoSignatarioDao(),
            db.campoAssinaturaDao(),
            db.eventoDao())
    }
    val usuarioRepository = remember { UsuarioRepository(db.usuarioDao()) }

    // Flow: a lista se atualiza sozinha quando alguém assina
    val documentos by documentoRepository.listarRecebidos(usuarioId).collectAsState(initial = emptyList())

    var primeiroNome by remember { mutableStateOf("") }
    LaunchedEffect(usuarioId) {
        primeiroNome = usuarioRepository.buscarPorId(usuarioId)?.nomeCompleto?.substringBefore(" ").orEmpty()
    }

    // rememberSaveable: a aba escolhida não se perde ao girar a tela
    var abaAssinados by rememberSaveable { mutableStateOf(false) }
    var selecionadoId by remember { mutableStateOf<Long?>(null) }

    // A aba depende da assinatura individual, não do status geral do documento
    val pendentes = documentos.filter { !it.assinado }
    val assinados = documentos.filter { it.assinado }
    val lista = if (abaAssinados) assinados else pendentes
    // Sem escolha, o primeiro da lista fica selecionado, como no protótipo
    val selecionado = lista.find { it.documentoId == selecionadoId } ?: lista.firstOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .background(FundoTela)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("Meus documentos", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextoPrincipal)
        Spacer(Modifier.height(24.dp))
        Text("Olá, $primeiroNome. Aqui estão suas assinaturas.", fontSize = 13.sp, color = TextoSecundario)
        Spacer(Modifier.height(16.dp))

        if (documentos.isEmpty()) {
            // Estado vazio do protótipo
            Text("Nenhum documento encontrado", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AzulLetsFlow)
            Text(
                "Quando houver um envio destinado a você, ele aparecerá aqui.",
                fontSize = 12.sp,
                color = TextoSecundario
            )
        } else {
            val total = if (abaAssinados) assinados.size else pendentes.size
            Text("%02d".format(total), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = AzulLetsFlow)
            Text(
                if (abaAssinados) "Documentos já assinados por você"
                else "Documentos aguardando você",
                fontSize = 13.sp,
                color = TextoSecundario
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Aba("PENDENTES", ativa = !abaAssinados) { abaAssinados = false }
            Aba("ASSINADOS", ativa = abaAssinados) { abaAssinados = true }
        }
        Spacer(Modifier.height(16.dp))

        // Tem documento, mas nenhum nesta aba
        if (documentos.isNotEmpty() && lista.isEmpty()) {
            Text(
                if (abaAssinados) "Você ainda não assinou nenhum documento."
                else "Nenhuma assinatura pendente.",
                fontSize = 13.sp,
                color = TextoSecundario
            )
            Spacer(Modifier.height(16.dp))
        }

        lista.forEach { documento ->
            key(documento.documentoId) {
                val ehSelecionado = documento.documentoId == selecionado?.documentoId
                CartaoDocumento(documento, ehSelecionado) { selecionadoId = documento.documentoId }

                // O botão de ação aparece só embaixo do documento selecionado
                if (ehSelecionado) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (documento.assinado) onVisualizar(documento.documentoId)
                            else onAssinar(documento.documentoId)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulLetsFlow)
                    ) {
                        Text(if (documento.assinado) "Visualizar documento"
                        else "Visualizar e assinar")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Text("Acesse apenas os documentos enviados para você.", fontSize = 12.sp, color = TextoSecundario)
        Spacer(Modifier.height(16.dp))
        Text(
            "Sair da conta",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextoPrincipal,
            modifier = Modifier.clickable(onClick = onSair).padding(vertical = 4.dp)
        )
    }
}

// Aba de texto com uma linha embaixo quando está ativa
@Composable
private fun Aba(texto: String, ativa: Boolean, onClick: () -> Unit) {
    // IntrinsicSize.Max: a linha fica do tamanho exato do texto
    Column(Modifier.width(IntrinsicSize.Max).clickable(onClick = onClick)) {
        Text(
            texto,
            fontSize = 13.sp,
            fontWeight = if (ativa) FontWeight.SemiBold else FontWeight.Normal,
            color = if (ativa) AzulLetsFlow else TextoSecundario,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (ativa) AzulLetsFlow
                else Color.Transparent)
        )
    }
}

@Composable
private fun CartaoDocumento(documento: DocumentoRecebido, selecionado: Boolean, onClick: () -> Unit) {
    val formato = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(formato) // Deixa o efeito do toque com os cantos arredondados
            .background(Color.White)
            .border(
                width = if (selecionado) 1.5.dp
                else 1.dp,
                color = if (selecionado) AzulLetsFlow
                else BordaCartao,
                shape = formato
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text("▤  ${documento.titulo}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextoPrincipal)
        Spacer(Modifier.height(6.dp))
        Text(
            "Recebido em ${formatarData(documento.dataRecebimento)} · ${textoCampos(documento.totalCampos)}",
            fontSize = 12.sp,
            color = TextoSecundario
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (documento.assinado) "Assinado"
            else "Pendente",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (documento.assinado) CorAssinado
            else CorPendente
        )
    }
}

// Formata a data como "28/10"
private val formatoData = SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR"))

private fun formatarData(millis: Long) = formatoData.format(Date(millis))

// "1 campo" ou "2 campos"
private fun textoCampos(total: Int) = if (total == 1) "1 campo"
else "$total campos"