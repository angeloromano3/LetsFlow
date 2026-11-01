package br.com.fiap.letsflow.ui.signatario

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.data.local.entity.Usuario
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

private const val TEXTO_PADRAO_DO_AVISO = "Confira os campos obrigatórios e o formato do e-mail."

private val formatoDeEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

// Serve para cadastrar (signatarioParaEditar nulo) e para editar um signatário existente
@Composable
fun FormularioSignatarioScreen(
    onSalvar: (nomeCompleto: String, email: String, telefone: String, senha: String) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    signatarioParaEditar: Usuario? = null,
    erroDoSalvamento: String? = null // Ex.: "E-mail já cadastrado", vindo do repository
) {
    val modoEdicao = signatarioParaEditar != null

    var nomeCompleto by rememberSaveable { mutableStateOf(signatarioParaEditar?.nomeCompleto.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(signatarioParaEditar?.email.orEmpty()) }
    var telefone by rememberSaveable { mutableStateOf(signatarioParaEditar?.telefone.orEmpty()) }
    var senha by rememberSaveable { mutableStateOf("") }
    // Os erros só aparecem depois da primeira tentativa de salvar
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }

    val nomeInvalido = nomeCompleto.isBlank()
    val emailInvalido = !formatoDeEmail.matches(email.trim())
    val telefoneInvalido = telefone.count { it.isDigit() } < 10
    // Ao editar, a senha só é exigida se a pessoa quiser trocá-la; o repository pede 6 ou mais
    val senhaInvalida = if (modoEdicao) {
        senha.isNotEmpty() && senha.length < 6
    } else {
        senha.length < 6
    }
    val algumCampoInvalido = nomeInvalido || emailInvalido || telefoneInvalido || senhaInvalida

    FormularioSignatarioConteudo(
        modoEdicao = modoEdicao,
        nomeCompleto = nomeCompleto,
        email = email,
        telefone = telefone,
        senha = senha,
        nomeComErro = tentouSalvar && nomeInvalido,
        emailComErro = tentouSalvar && emailInvalido,
        telefoneComErro = tentouSalvar && telefoneInvalido,
        senhaComErro = tentouSalvar && senhaInvalida,
        textoDoAviso = when {
            erroDoSalvamento != null -> erroDoSalvamento
            tentouSalvar && algumCampoInvalido -> TEXTO_PADRAO_DO_AVISO
            else -> null
        },
        aoMudarNome = { nomeCompleto = it },
        aoMudarEmail = { email = it },
        aoMudarTelefone = { telefone = it },
        aoMudarSenha = { senha = it },
        aoSalvar = {
            tentouSalvar = true
            if (!algumCampoInvalido) {
                onSalvar(nomeCompleto.trim(), email.trim(), telefone.trim(), senha)
            }
        },
        aoCancelar = onCancelar,
        modifier = modifier
    )
}

// Só desenha a tela, sem estado: assim o Preview mostra cada situação do Figma
@Composable
private fun FormularioSignatarioConteudo(
    modoEdicao: Boolean,
    nomeCompleto: String,
    email: String,
    telefone: String,
    senha: String,
    nomeComErro: Boolean,
    emailComErro: Boolean,
    telefoneComErro: Boolean,
    senhaComErro: Boolean,
    textoDoAviso: String?,
    aoMudarNome: (String) -> Unit,
    aoMudarEmail: (String) -> Unit,
    aoMudarTelefone: (String) -> Unit,
    aoMudarSenha: (String) -> Unit,
    aoSalvar: () -> Unit,
    aoCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores
    val tipografia = MaterialTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            // Rolagem evita cortar campos quando o teclado abre
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = cores.texto,
                modifier = Modifier
                    .semantics { contentDescription = "Voltar" }
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { aoCancelar() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (modoEdicao) "Editar signatário" else "Novo signatário",
                style = tipografia.headlineMedium,
                color = cores.texto
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (modoEdicao) {
                "Atualize os dados do signatário."
            } else {
                "Preencha os dados para criar uma conta."
            },
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        if (textoDoAviso != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Revise o cadastro",
                style = tipografia.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = cores.erro
            )
            Text(
                text = textoDoAviso,
                style = tipografia.bodySmall,
                color = cores.textoSecundario
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CampoDoFormulario(
            rotulo = "Nome completo *",
            valor = nomeCompleto,
            aoMudarValor = aoMudarNome,
            placeholder = "Ex.: Nome e sobrenome",
            comErro = nomeComErro,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        CampoDoFormulario(
            rotulo = "E-mail *",
            valor = email,
            aoMudarValor = aoMudarEmail,
            placeholder = "nome@exemplo.com",
            comErro = emailComErro,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        CampoDoFormulario(
            rotulo = "Telefone *",
            valor = telefone,
            aoMudarValor = aoMudarTelefone,
            placeholder = "(11) 99999-9999",
            comErro = telefoneComErro,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        CampoDoFormulario(
            rotulo = "Senha *",
            valor = senha,
            aoMudarValor = aoMudarSenha,
            placeholder = if (modoEdicao) "••••••••" else "Crie uma senha",
            comErro = senhaComErro,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { aoSalvar() })
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (modoEdicao) {
                "A senha existente não é mostrada. Preencha somente para alterá-la."
            } else {
                "Use uma senha segura. Ela não será exibida depois."
            },
            style = tipografia.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(16.dp))

        BotaoPrimario(
            texto = if (modoEdicao) "Salvar alterações" else "Salvar signatário",
            onClick = aoSalvar,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Cancelar e voltar à lista",
            style = tipografia.bodySmall,
            color = cores.acao,
            modifier = Modifier
                .clickable(role = Role.Button) { aoCancelar() }
                .padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CampoDoFormulario(
    rotulo: String,
    valor: String,
    aoMudarValor: (String) -> Unit,
    placeholder: String,
    comErro: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val cores = LetsFlowTheme.cores
    val formaDoCampo = RoundedCornerShape(12.dp)

    Column(modifier = modifier) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelLarge,
            color = cores.texto
        )

        Spacer(modifier = Modifier.height(8.dp))

        // BasicTextField permite a altura compacta do Figma (OutlinedTextField tem mínimo de 56dp)
        BasicTextField(
            value = valor,
            onValueChange = aoMudarValor,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (comErro) cores.erro else cores.texto
            ),
            cursorBrush = SolidColor(cores.acao),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(cores.superficie, formaDoCampo)
                .border(1.dp, if (comErro) cores.erro else cores.divisao, formaDoCampo)
                .padding(horizontal = 16.dp),
            decorationBox = { campoInterno ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (valor.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cores.textoSecundario
                        )
                    }
                    campoInterno()
                }
            }
        )
    }
}

@Preview(name = "Novo", showBackground = true, showSystemUi = true)
@Composable
private fun FormularioNovoPreview() {
    LetsFlowTheme {
        FormularioSignatarioConteudo(
            modoEdicao = false,
            nomeCompleto = "",
            email = "",
            telefone = "",
            senha = "",
            nomeComErro = false,
            emailComErro = false,
            telefoneComErro = false,
            senhaComErro = false,
            textoDoAviso = null,
            aoMudarNome = {},
            aoMudarEmail = {},
            aoMudarTelefone = {},
            aoMudarSenha = {},
            aoSalvar = {},
            aoCancelar = {}
        )
    }
}

@Preview(name = "Editar", showBackground = true, showSystemUi = true)
@Composable
private fun FormularioEditarPreview() {
    LetsFlowTheme {
        FormularioSignatarioConteudo(
            modoEdicao = true,
            nomeCompleto = "Mariana Oliveira",
            email = "mariana@email.com",
            telefone = "(11) 99999-9999",
            senha = "",
            nomeComErro = false,
            emailComErro = false,
            telefoneComErro = false,
            senhaComErro = false,
            textoDoAviso = null,
            aoMudarNome = {},
            aoMudarEmail = {},
            aoMudarTelefone = {},
            aoMudarSenha = {},
            aoSalvar = {},
            aoCancelar = {}
        )
    }
}

@Preview(name = "Dados inválidos", showBackground = true, showSystemUi = true)
@Composable
private fun FormularioInvalidoPreview() {
    LetsFlowTheme {
        FormularioSignatarioConteudo(
            modoEdicao = false,
            nomeCompleto = "Mariana Oliveira",
            email = "mariana@email",
            telefone = "",
            senha = "",
            nomeComErro = false,
            emailComErro = true,
            telefoneComErro = true,
            senhaComErro = false,
            textoDoAviso = TEXTO_PADRAO_DO_AVISO,
            aoMudarNome = {},
            aoMudarEmail = {},
            aoMudarTelefone = {},
            aoMudarSenha = {},
            aoSalvar = {},
            aoCancelar = {}
        )
    }
}