package br.com.fiap.letsflow.ui.signatario

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.repository.UsuarioRepository

// Liga a ListaSignatariosScreen ao banco: busca os signatários e repassa para a tela
@Composable
fun ListaSignatariosRota(
    onVoltar: () -> Unit,
    onCadastrar: () -> Unit,
    onEditar: (usuarioId: Long) -> Unit, // Devolve só o id: a navegação passa ids entre as telas
    modifier: Modifier = Modifier,
    mostrarAvisoSalvo: Boolean = false // Liga depois de salvar um cadastro
) {
    val context = LocalContext.current
    val usuarioRepository = remember { UsuarioRepository(AppDatabase.getInstance(context).usuarioDao()) }

    // remember guarda o Flow; o Room emite a lista de novo sempre que alguém é cadastrado ou editado
    val fluxoSignatarios = remember { usuarioRepository.listarSignatarios() }
    val signatarios by fluxoSignatarios.collectAsState(initial = emptyList())

    ListaSignatariosScreen(
        signatarios = signatarios,
        onVoltar = onVoltar,
        onCadastrar = onCadastrar,
        onEditar = { usuario -> onEditar(usuario.id) },
        modifier = modifier,
        mostrarAvisoSalvo = mostrarAvisoSalvo
    )
}