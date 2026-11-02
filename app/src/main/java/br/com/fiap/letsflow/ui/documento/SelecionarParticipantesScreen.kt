package br.com.fiap.letsflow.ui.documento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.componentes.BotaoSecundario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

// Uma pessoa já escolhida para assinar o documento, com o papel dela
data class ParticipanteSelecionadoUi(
    val id: Long,
    val nome: String,
    val email: String,
    val papel: String // Por exemplo "Contratante"
)

private val papeisPadrao = listOf("Contratante", "Contratado", "Testemunha")

// Etapa 2 do fluxo: quem vai assinar e qual o papel de cada pessoa
@Composable
fun SelecionarParticipantesScreen(
    participantes: List<ParticipanteSelecionadoUi>,
    onEscolherPapel: (participante: ParticipanteSelecionadoUi, papel: String) -> Unit,
    onSubstituir: (ParticipanteSelecionadoUi) -> Unit,
    onRemover: (ParticipanteSelecionadoUi) -> Unit,
    onCadastrarOutraPessoa: () -> Unit,
    onContinuar: () -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    opcoesDePapel: List<String> = papeisPadrao,
    participanteAtualizado: Boolean = false, // Liga o aviso depois de substituir uma pessoa
    onBuscar: (String) -> Unit = {}
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var idComPapelAberto by rememberSaveable { mutableStateOf<Long?>(null) }

    SelecionarParticipantesConteudo(
        participantes = participantes,
        opcoesDePapel = opcoesDePapel,
        busca = busca,
        idComPapelAberto = idComPapelAberto,
        participanteAtualizado = participanteAtualizado,
        aoMudarBusca = {
            busca = it
            onBuscar(it)
        },
        aoAlternarPapel = { id -> idComPapelAberto = if (idComPapelAberto == id) null else id },
        aoEscolherPapel = { participante, papel ->
            idComPapelAberto = null // Fecha a lista de papéis depois de escolher
            onEscolherPapel(participante, papel)
        },
        onSubstituir = onSubstituir,
        onRemover = onRemover,
        onCadastrarOutraPessoa = onCadastrarOutraPessoa,
        onContinuar = onContinuar,
        onVoltar = onVoltar,
        modifier = modifier
    )
}

// Separado da tela para o Preview mostrar cada estado do Figma
@Composable
private fun SelecionarParticipantesConteudo(
    participantes: List<ParticipanteSelecionadoUi>,
    opcoesDePapel: List<String>,
    busca: String,
    idComPapelAberto: Long?,
    participanteAtualizado: Boolean,
    aoMudarBusca: (String) -> Unit,
    aoAlternarPapel: (Long) -> Unit,
    aoEscolherPapel: (ParticipanteSelecionadoUi, String) -> Unit,
    onSubstituir: (ParticipanteSelecionadoUi) -> Unit,
    onRemover: (ParticipanteSelecionadoUi) -> Unit,
    onCadastrarOutraPessoa: () -> Unit,
    onContinuar: () -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography
    val semParticipantes = participantes.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            CabecalhoSelecionarParticipantes(onVoltar = onVoltar)

            Spacer(modifier = Modifier.height(16.dp))

            if (semParticipantes) {
                AvisoSemParticipantes()
                Spacer(modifier = Modifier.height(16.dp))
            } else if (participanteAtualizado) {
                AvisoParticipanteAtualizado()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Selecione pelo menos uma pessoa e defina o papel dela no documento.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Buscar signatários *",
                style = tipografia.labelLarge,
                color = cores.texto
            )

            Spacer(modifier = Modifier.height(8.dp))

            CampoDeBuscaDeSignatarios(valor = busca, aoMudarValor = aoMudarBusca)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Selecionados · ${participantes.size}",
                style = tipografia.titleLarge,
                color = cores.texto
            )

            Spacer(modifier = Modifier.height(16.dp))

            participantes.forEach { participante ->
                LinhaDeParticipante(
                    participante = participante,
                    aoAlternarPapel = { aoAlternarPapel(participante.id) },
                    onSubstituir = { onSubstituir(participante) },
                    onRemover = { onRemover(participante) }
                )

                if (participante.id == idComPapelAberto) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ListaDePapeis(
                        participante = participante,
                        opcoesDePapel = opcoesDePapel,
                        aoEscolher = { papel -> aoEscolherPapel(participante, papel) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            BotaoSecundario(
                texto = "+  Cadastrar outra pessoa",
                onClick = onCadastrarOutraPessoa,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Papéis: contratante, contratado e testemunha.",
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Botão fixo no rodapé, fora da rolagem
        BotaoPrimario(
            texto = "Continuar para os campos →",
            onClick = onContinuar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            habilitado = !semParticipantes
        )
    }
}

@Composable
private fun CabecalhoSelecionarParticipantes(onVoltar: () -> Unit) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "←",
            fontSize = 22.sp,
            color = cores.texto,
            modifier = Modifier
                .semantics { contentDescription = "Voltar" }
                .clip(CircleShape)
                .clickable(role = Role.Button) { onVoltar() }
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Selecionar participantes",
            style = tipografia.headlineMedium,
            color = cores.texto
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Etapa 2 de 4 · Quem vai assinar?",
        style = tipografia.bodySmall,
        color = cores.textoSecundario
    )
}

@Composable
private fun AvisoSemParticipantes() {
    val cores = LetsFlowTheme.cores

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cores.fundoErro)
            .padding(12.dp)
    ) {
        Text(
            text = "Selecione pelo menos uma pessoa",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = cores.erro
        )
        Text(
            text = "Um documento não pode ser enviado sem signatários.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )
    }
}

@Composable
private fun AvisoParticipanteAtualizado() {
    val cores = LetsFlowTheme.cores

    Column {
        Text(
            text = "Participante atualizado",
            style = MaterialTheme.typography.titleLarge,
            color = cores.acao
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Os campos de assinatura vinculados precisam ser conferidos.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )
    }
}

@Composable
private fun CampoDeBuscaDeSignatarios(
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
private fun LinhaDeParticipante(
    participante: ParticipanteSelecionadoUi,
    aoAlternarPapel: () -> Unit,
    onSubstituir: () -> Unit,
    onRemover: () -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(cores.destaque),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iniciaisDoParticipante(participante.nome),
                    color = cores.acao,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = participante.nome,
                    style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = cores.texto
                )
                Text(
                    text = participante.email,
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
                Text(
                    text = "Papel: ${participante.papel}",
                    style = tipografia.bodySmall,
                    color = cores.textoSecundario
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Três ações em colunas iguais, como no Figma
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Papel",
                style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = cores.acao,
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button) { aoAlternarPapel() }
                    .padding(vertical = 8.dp)
            )
            Text(
                text = "Substituir",
                style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = cores.acao,
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button) { onSubstituir() }
                    .padding(vertical = 8.dp)
            )
            Text(
                text = "Remover",
                style = tipografia.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = cores.erro,
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button) { onRemover() }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ListaDePapeis(
    participante: ParticipanteSelecionadoUi,
    opcoesDePapel: List<String>,
    aoEscolher: (String) -> Unit
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cores.superficie)
            .padding(16.dp)
    ) {
        Text(
            text = "Papel de ${participante.nome}",
            style = tipografia.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = cores.texto
        )

        opcoesDePapel.forEach { papel ->
            val escolhido = papel == participante.papel

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (escolhido) "$papel ✓" else papel,
                style = tipografia.bodyMedium.copy(
                    fontWeight = if (escolhido) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (escolhido) cores.acao else cores.texto,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (escolhido) cores.destaque else cores.fundo)
                    .clickable(role = Role.Button) { aoEscolher(papel) }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
    }
}

// "Mariana Oliveira" vira "MO"; com um nome só, usa as duas primeiras letras
private fun iniciaisDoParticipante(nomeCompleto: String): String {
    val partes = nomeCompleto.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        partes.isEmpty() -> ""
        partes.size == 1 -> partes.first().take(2).uppercase()
        else -> "${partes.first().first()}${partes.last().first()}".uppercase()
    }
}

private fun participantesDeExemplo() = listOf(
    ParticipanteSelecionadoUi(
        id = 1,
        nome = "Mariana Oliveira",
        email = "mariana@email.com",
        papel = "Contratante"
    ),
    ParticipanteSelecionadoUi(
        id = 2,
        nome = "Carlos Almeida",
        email = "carlos@email.com",
        papel = "Contratado"
    )
)

@Preview(name = "Dois selecionados", showBackground = true, showSystemUi = true)
@Composable
private fun SelecionarDoisPreview() {
    LetsFlowTheme {
        SelecionarParticipantesConteudo(
            participantes = participantesDeExemplo(),
            opcoesDePapel = papeisPadrao,
            busca = "",
            idComPapelAberto = null,
            participanteAtualizado = false,
            aoMudarBusca = {},
            aoAlternarPapel = {},
            aoEscolherPapel = { _, _ -> },
            onSubstituir = {},
            onRemover = {},
            onCadastrarOutraPessoa = {},
            onContinuar = {},
            onVoltar = {}
        )
    }
}

@Preview(name = "Seleção vazia", showBackground = true, showSystemUi = true)
@Composable
private fun SelecionarVazioPreview() {
    LetsFlowTheme {
        SelecionarParticipantesConteudo(
            participantes = emptyList(),
            opcoesDePapel = papeisPadrao,
            busca = "",
            idComPapelAberto = null,
            participanteAtualizado = false,
            aoMudarBusca = {},
            aoAlternarPapel = {},
            aoEscolherPapel = { _, _ -> },
            onSubstituir = {},
            onRemover = {},
            onCadastrarOutraPessoa = {},
            onContinuar = {},
            onVoltar = {}
        )
    }
}

@Preview(name = "Escolher papel", showBackground = true, showSystemUi = true)
@Composable
private fun SelecionarEscolherPapelPreview() {
    LetsFlowTheme {
        SelecionarParticipantesConteudo(
            participantes = participantesDeExemplo(),
            opcoesDePapel = papeisPadrao,
            busca = "",
            idComPapelAberto = 1,
            participanteAtualizado = false,
            aoMudarBusca = {},
            aoAlternarPapel = {},
            aoEscolherPapel = { _, _ -> },
            onSubstituir = {},
            onRemover = {},
            onCadastrarOutraPessoa = {},
            onContinuar = {},
            onVoltar = {}
        )
    }
}

@Preview(name = "Pessoa substituída", showBackground = true, showSystemUi = true)
@Composable
private fun SelecionarPessoaSubstituidaPreview() {
    LetsFlowTheme {
        SelecionarParticipantesConteudo(
            participantes = listOf(
                participantesDeExemplo().first(),
                ParticipanteSelecionadoUi(
                    id = 3,
                    nome = "Beatriz Santos",
                    email = "beatriz@email.com",
                    papel = "Contratado"
                )
            ),
            opcoesDePapel = papeisPadrao,
            busca = "",
            idComPapelAberto = null,
            participanteAtualizado = true,
            aoMudarBusca = {},
            aoAlternarPapel = {},
            aoEscolherPapel = { _, _ -> },
            onSubstituir = {},
            onRemover = {},
            onCadastrarOutraPessoa = {},
            onContinuar = {},
            onVoltar = {}
        )
    }
}