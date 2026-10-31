package br.com.fiap.letsflow.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.R
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.Usuario
import br.com.fiap.letsflow.data.repository.UsuarioRepository
import br.com.fiap.letsflow.ui.componentes.BotaoPrimario
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onEntrou: (Usuario) -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val usuarioRepository = remember {
        UsuarioRepository(AppDatabase.getInstance(contexto).usuarioDao())
    }
    val escopo = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mostrarSenha by remember { mutableStateOf(false) }
    var entrando by remember { mutableStateOf(false) }
    var falhaNoLogin by remember { mutableStateOf(false) }

    val podeEntrar = email.isNotEmpty() && senha.isNotEmpty() && !entrando

    fun entrar() {
        if (!podeEntrar) return
        escopo.launch {
            entrando = true
            falhaNoLogin = false
            try {
                // O repository já faz trim e lowercase do e-mail
                val usuario = usuarioRepository.login(email, senha)
                if (usuario != null) {
                    onEntrou(usuario)
                } else {
                    falhaNoLogin = true
                }
            } finally {
                // Garante que o botão volte ao normal mesmo se der erro
                entrando = false
            }
        }
    }

    LoginConteudo(
        email = email,
        senha = senha,
        mostrarSenha = mostrarSenha,
        podeEntrar = podeEntrar,
        entrando = entrando,
        falhaNoLogin = falhaNoLogin,
        aoMudarEmail = {
            email = it
            falhaNoLogin = false
        },
        aoMudarSenha = {
            senha = it
            falhaNoLogin = false
        },
        aoAlternarSenha = { mostrarSenha = !mostrarSenha },
        aoEntrar = { entrar() },
        modifier = modifier
    )
}

// Só desenha a tela, sem banco nem lógica: assim o Preview funciona
@Composable
private fun LoginConteudo(
    email: String,
    senha: String,
    mostrarSenha: Boolean,
    podeEntrar: Boolean,
    entrando: Boolean,
    falhaNoLogin: Boolean,
    aoMudarEmail: (String) -> Unit,
    aoMudarSenha: (String) -> Unit,
    aoAlternarSenha: () -> Unit,
    aoEntrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = LetsFlowTheme.cores

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cores.fundo)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            // Rolagem evita cortar campos quando o teclado abre
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo_letsflow),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "LETSFLOW",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = cores.acao
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Assinaturas com clareza, do começo ao fim.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(16.dp))

        // A caixa de erro fica acima do título, como no Figma
        if (falhaNoLogin) {
            CaixaDeErro()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Entre na sua conta",
            style = MaterialTheme.typography.headlineMedium,
            color = cores.texto
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Acesse seus documentos de forma simples e segura.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(20.dp))

        CampoDeTexto(
            rotulo = "E-mail *",
            valor = email,
            aoMudarValor = aoMudarEmail,
            placeholder = "seuemail@exemplo.com",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        CampoDeTexto(
            rotulo = "Senha *",
            valor = senha,
            aoMudarValor = aoMudarSenha,
            placeholder = "••••••••",
            visualTransformation = if (mostrarSenha) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { aoEntrar() })
        )

        Text(
            text = if (mostrarSenha) "Ocultar senha" else "Mostrar senha",
            style = MaterialTheme.typography.bodySmall,
            color = cores.acao,
            modifier = Modifier
                .clickable(role = Role.Button) { aoAlternarSenha() }
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        BotaoPrimario(
            texto = if (entrando) "Entrando..." else "Entrar",
            onClick = aoEntrar,
            modifier = Modifier.fillMaxWidth(),
            habilitado = podeEntrar
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Acesso seguro para administradores e signatários",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CaixaDeErro() {
    val cores = LetsFlowTheme.cores

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cores.fundoErro)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Não foi possível entrar",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = cores.erro
        )
        Text(
            text = "Confira o e-mail e a senha e tente novamente.",
            style = MaterialTheme.typography.bodySmall,
            color = cores.textoSecundario
        )
    }
}

@Composable
private fun CampoDeTexto(
    rotulo: String,
    valor: String,
    aoMudarValor: (String) -> Unit,
    placeholder: String,
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
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = cores.texto),
            cursorBrush = SolidColor(cores.acao),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
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

@Preview(name = "Vazio", showBackground = true, showSystemUi = true)
@Composable
private fun LoginVazioPreview() {
    LetsFlowTheme {
        LoginConteudo(
            email = "",
            senha = "",
            mostrarSenha = false,
            podeEntrar = false,
            entrando = false,
            falhaNoLogin = false,
            aoMudarEmail = {},
            aoMudarSenha = {},
            aoAlternarSenha = {},
            aoEntrar = {}
        )
    }
}

@Preview(name = "Senha visível", showBackground = true, showSystemUi = true)
@Composable
private fun LoginSenhaVisivelPreview() {
    LetsFlowTheme {
        LoginConteudo(
            email = "mariana@email.com",
            senha = "SenhaTeste2026",
            mostrarSenha = true,
            podeEntrar = true,
            entrando = false,
            falhaNoLogin = false,
            aoMudarEmail = {},
            aoMudarSenha = {},
            aoAlternarSenha = {},
            aoEntrar = {}
        )
    }
}

@Preview(name = "Erro", showBackground = true, showSystemUi = true)
@Composable
private fun LoginErroPreview() {
    LetsFlowTheme {
        LoginConteudo(
            email = "seuemail@exemplo.com",
            senha = "123",
            mostrarSenha = false,
            podeEntrar = true,
            entrando = false,
            falhaNoLogin = true,
            aoMudarEmail = {},
            aoMudarSenha = {},
            aoAlternarSenha = {},
            aoEntrar = {}
        )
    }
}