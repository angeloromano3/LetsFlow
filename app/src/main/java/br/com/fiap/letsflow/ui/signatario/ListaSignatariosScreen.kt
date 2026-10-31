package br.com.fiap.letsflow.ui.signatario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.entity.Perfil
import br.com.fiap.letsflow.data.local.entity.Usuario
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// Tela do Admin com as pessoas que podem assinar. Recebe a lista pronta, sem repository
@Composable
fun ListaSignatariosScreen(
    signatarios: List<Usuario>,
    onVoltar: () -> Unit,
    onCadastrar: () -> Unit,
    onEditar: (Usuario) -> Unit,
    modifier: Modifier = Modifier,
    mostrarAvisoSalvo: Boolean = false // Liga depois de salvar um cadastro
) {
    var busca by rememberSaveable { mutableStateOf("") }

    ListaSignatariosConteudo(
        signatarios = signatarios,
        busca = busca,
        mostrarAvisoSalvo = mostrarAvisoSalvo,
        aoMudarBusca = { busca = it },
        onVoltar = onVoltar,
        onCadastrar = onCadastrar,
        onEditar = onEditar,
        modifier = modifier
    )
}

// Separado da tela para o Preview poder mostrar a busca preenchida
@Composable
private fun ListaSignatariosConteudo(
    signatarios: List<Usuario>,
    busca: String,
    mostrarAvisoSalvo: Boolean,
    aoMudarBusca: (String) -> Unit,
    onVoltar: () -> Unit,
    onCadastrar: () -> Unit,
    onEditar: (Usuario) -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    // Busca por nome ou e-mail, sem diferenciar maiúscula de minúscula
    val textoBusca = busca.trim()
    val signatariosFiltrados = signatarios.filter {
        textoBusca.isEmpty() ||
                it.nomeCompleto.contains(textoBusca, ignoreCase = true) ||
                it.email.contains(textoBusca, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SetaVoltar(onVoltar = onVoltar)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Signatários",
                    style = tipografia.headlineMedium,
                    color = cores.texto
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gerencie as pessoas que poderão assinar",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (mostrarAvisoSalvo) {
                AvisoSalvo()
                Spacer(modifier = Modifier.height(12.dp))
            }

            BotaoPrimario(
                texto = "+  Cadastrar signatário",
                onClick = onCadastrar,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Buscar *",
                style = tipografia.labelLarge,
                color = cores.texto
            )

            Spacer(modifier = Modifier.height(8.dp))

            CampoDeBusca(valor = busca, aoMudarValor = aoMudarBusca)

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (signatariosFiltrados.isEmpty()) {
            item {
                Text(
                    text = "Nenhum signatário encontrado.",
                    style = tipografia.bodyMedium,
                    color = cores.textoSecundario,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }

        items(signatariosFiltrados, key = { it.id }) { signatario ->
            CartaoDeSignatario(
                signatario = signatario,
                aoTocar = { onEditar(signatario) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Toque em um contato para editar seus dados.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SetaVoltar(onVoltar: () -> Unit) {
    Text(
        text = "←",
        fontSize = 22.sp,
        color = LetsFlowTheme.cores.texto,
        modifier = Modifier
            .semantics { contentDescription = "Voltar" }
            .clip(CircleShape)
            .clickable(role = Role.Button) { onVoltar() }
            .padding(8.dp)
    )
}

@Composable
private fun AvisoSalvo() {
    val cores = LetsFlowTheme.cores

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cores.destaque)
            .padding(12.dp)
    ) {
        Text(
            text = "Signatário salvo",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = cores.acao
        )
        Text(
            text = "O cadastro foi atualizado e já está disponível para seleção.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )
    }
}

@Composable
private fun CampoDeBusca(
    valor: String,
    aoMudarValor: (String) -> Unit
) {
    val cores = LetsFlowTheme.cores
    val formaDoCampo = RoundedCornerShape(12.dp)

    BasicTextField(
        value = valor,
        onValueChange = aoMudarValor,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = cores.texto),
        cursorBrush = SolidColor(cores.acao),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(cores.superficie, formaDoCampo)
            .border(1.dp, cores.divisao, formaDoCampo)
            .padding(horizontal = 16.dp),
        decorationBox = { campoInterno ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (valor.isEmpty()) {
                    Text(
                        text = "Nome ou e-mail",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.textoSecundario
                    )
                }
                campoInterno()
            }
        }
    )
}

@Composable
private fun CartaoDeSignatario(
    signatario: Usuario,
    aoTocar: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val formato = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(formato)
            .background(cores.superficie)
            .border(1.dp, cores.divisao, formato)
            .clickable(role = Role.Button) { aoTocar() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(cores.destaque),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iniciaisDoNome(signatario.nomeCompleto),
                color = cores.acao,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = signatario.nomeCompleto,
                style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cores.texto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Nome comprido corta com "..."
            )
            Text(
                text = signatario.email,
                style = tipografia.bodySmall,
                color = cores.textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// "Mariana Oliveira" vira "MO"; com um nome só, usa as duas primeiras letras
private fun iniciaisDoNome(nomeCompleto: String): String {
    val partes = nomeCompleto.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        partes.isEmpty() -> ""
        partes.size == 1 -> partes.first().take(2).uppercase()
        else -> "${partes.first().first()}${partes.last().first()}".uppercase()
    }
}

private fun signatariosDeExemplo(): List<Usuario> = listOf(
    Usuario(
        id = 1,
        nomeCompleto = "Mariana Oliveira",
        email = "mariana@email.com",
        telefone = "(11) 99999-9999",
        senha = "",
        perfil = Perfil.SIGNATARIO
    ),
    Usuario(
        id = 2,
        nomeCompleto = "Carlos Almeida",
        email = "carlos@email.com",
        telefone = "(11) 98888-8888",
        senha = "",
        perfil = Perfil.SIGNATARIO
    ),
    Usuario(
        id = 3,
        nomeCompleto = "Beatriz Santos",
        email = "beatriz@email.com",
        telefone = "(11) 97777-7777",
        senha = "",
        perfil = Perfil.SIGNATARIO
    ),
    Usuario(
        id = 4,
        nomeCompleto = "Luiza Ferreira",
        email = "luiza@email.com",
        telefone = "(11) 96666-6666",
        senha = "",
        perfil = Perfil.SIGNATARIO
    )
)

@Preview(name = "Lista", showBackground = true, showSystemUi = true)
@Composable
private fun ListaSignatariosPreview() {
    LetsFlowTheme {
        ListaSignatariosConteudo(
            signatarios = signatariosDeExemplo(),
            busca = "",
            mostrarAvisoSalvo = false,
            aoMudarBusca = {},
            onVoltar = {},
            onCadastrar = {},
            onEditar = {}
        )
    }
}

@Preview(name = "Busca filtrada", showBackground = true, showSystemUi = true)
@Composable
private fun ListaSignatariosBuscaPreview() {
    LetsFlowTheme {
        ListaSignatariosConteudo(
            signatarios = signatariosDeExemplo(),
            busca = "Mariana",
            mostrarAvisoSalvo = false,
            aoMudarBusca = {},
            onVoltar = {},
            onCadastrar = {},
            onEditar = {}
        )
    }
}

@Preview(name = "Cadastro salvo", showBackground = true, showSystemUi = true)
@Composable
private fun ListaSignatariosSalvoPreview() {
    LetsFlowTheme {
        ListaSignatariosConteudo(
            signatarios = signatariosDeExemplo(),
            busca = "",
            mostrarAvisoSalvo = true,
            aoMudarBusca = {},
            onVoltar = {},
            onCadastrar = {},
            onEditar = {}
        )
    }
}