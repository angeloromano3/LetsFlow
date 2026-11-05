package br.com.fiap.letsflow.ui.signatario

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.Usuario
import br.com.fiap.letsflow.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

// Liga o FormularioSignatarioScreen ao repository.
// signatarioId nulo = cadastrar; com id = editar aquele signatário.
// Depois de salvar chama onSalvo: quem navega decide para onde voltar (a lista com o aviso
// "Signatário salvo", ou a tela de novo documento, quando o cadastro veio de lá)
@Composable
fun FormularioSignatarioRota(
    onSalvo: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    signatarioId: Long? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val usuarioRepository = remember { UsuarioRepository(db.usuarioDao()) }

    var signatario by remember { mutableStateOf<Usuario?>(null) }
    var carregado by remember { mutableStateOf(signatarioId == null) } // Cadastro novo não tem o que carregar
    var erro by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) } // Evita salvar duas vezes com toques seguidos

    // O formulário preenche os campos só na primeira composição, por isso a edição
    // só é desenhada depois que o signatário chegou do banco
    LaunchedEffect(signatarioId) {
        if (signatarioId != null) {
            signatario = usuarioRepository.buscarPorId(signatarioId)
            carregado = true
            if (signatario == null) onCancelar() // Id que não existe mais: volta para a lista
        }
    }

    if (!carregado || (signatarioId != null && signatario == null)) return

    FormularioSignatarioScreen(
        onSalvar = { nome, email, telefone, senha ->
            if (!salvando) {
                salvando = true
                erro = null
                scope.launch {
                    val resultado: Result<Unit> = if (signatarioId == null) {
                        usuarioRepository.cadastrarSignatario(nome, email, telefone, senha).map { }
                    } else {
                        usuarioRepository.atualizarSignatario(signatarioId, nome, email, telefone, senha)
                    }
                    resultado
                        .onSuccess { onSalvo() }
                        .onFailure { erro = it.message } // Ex.: "E-mail já cadastrado"
                    salvando = false
                }
            }
        },
        onCancelar = onCancelar,
        modifier = modifier,
        signatarioParaEditar = signatario,
        erroDoSalvamento = erro
    )
}
