package br.com.fiap.letsflow.ui.navegacao

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.letsflow.data.local.AppDatabase
import br.com.fiap.letsflow.data.local.entity.Perfil
import br.com.fiap.letsflow.data.local.entity.Usuario
import br.com.fiap.letsflow.data.repository.DocumentoRepository
import br.com.fiap.letsflow.ui.documento.DocumentosRota
import br.com.fiap.letsflow.ui.login.LoginScreen
import br.com.fiap.letsflow.ui.assinatura.DocumentosRecebidosScreen


import androidx.compose.material3.Text
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.ui.documento.NovoDocumentoScreen
import br.com.fiap.letsflow.ui.documento.PosicionamentoScreen


import br.com.fiap.letsflow.ui.documento.RevisarEnviarScreen
import br.com.fiap.letsflow.ui.documento.ResumoDocumentoUi
import br.com.fiap.letsflow.ui.documento.ParticipanteResumoUi
import br.com.fiap.letsflow.ui.documento.rotulo
import br.com.fiap.letsflow.data.local.storage.RenderizadorPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File



import br.com.fiap.letsflow.ui.documento.DocumentoEnviadoScreen
import br.com.fiap.letsflow.ui.documento.DetalhesDocumentoScreen


import br.com.fiap.letsflow.ui.documento.VisualizarDocumentoAdminRoute


import br.com.fiap.letsflow.ui.signatario.FormularioSignatarioRota
import br.com.fiap.letsflow.ui.signatario.ListaSignatariosRota
import android.widget.Toast

import br.com.fiap.letsflow.ui.assinatura.AssinaturaScreen
import br.com.fiap.letsflow.ui.assinatura.VisualizarDocumentoScreen as VisualizarDocumentoSignatarioScreen



// Chave usada para o formulário avisar a tela anterior que um signatário foi salvo
private const val CHAVE_SIGNATARIO_SALVO = "signatarioSalvo"

@Composable
fun LetsFlowNavHost() {

    val navController = rememberNavController()
    val contexto = LocalContext.current

    val banco = remember(contexto) {
        AppDatabase.getInstance(contexto)
    }

    val documentoRepository = remember(banco) {
        DocumentoRepository(
            banco.documentoDao(),
            banco.documentoSignatarioDao(),
            banco.campoAssinaturaDao(),
            banco.eventoDao()
        )
    }

    // Guarda o usuário autenticado durante a execução do app.
    var usuarioLogado by remember {
        mutableStateOf<Usuario?>(null)
    }

    val sair: () -> Unit = {
        usuarioLogado = null

        navController.navigate(Rotas.LOGIN) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Rotas.LOGIN
    ) {

        // 1. LOGIN
        composable(Rotas.LOGIN) {

            LoginScreen(
                onEntrou = { usuario ->

                    usuarioLogado = usuario

                    val destino = when (usuario.perfil) {
                        Perfil.ADMIN -> Rotas.INICIO_ADMIN
                        Perfil.SIGNATARIO ->
                            Rotas.documentosRecebidos(usuario.id)
                    }

                    navController.navigate(destino) {
                        popUpTo(Rotas.LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 2. INÍCIO DO ADMIN
        composable(Rotas.INICIO_ADMIN) {

            val admin = usuarioLogado

            if (admin?.perfil == Perfil.ADMIN) {

                // Rota da Ani: busca os documentos do Admin com o "X de Y" e mostra a tela da Mari
                DocumentosRota(
                    adminId = admin.id,

                    onNovoDocumento = {
                        navController.navigate(
                            Rotas.NOVO_DOCUMENTO
                        )
                    },

                    onAbrirDocumento = { documento ->
                        navController.navigate(
                            Rotas.acompanhamento(documento.id)
                        )
                    },

                    onIrParaSignatarios = {
                        navController.navigate(
                            Rotas.LISTA_SIGNATARIOS
                        )
                    },

                    onSair = sair
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 4. NOVO DOCUMENTO
        composable(Rotas.NOVO_DOCUMENTO) {

            val admin = usuarioLogado

            if (admin?.perfil == Perfil.ADMIN) {

                NovoDocumentoScreen(
                    adminId = admin.id,

                    onSalvo = { documentoId ->

                        navController.navigate(
                            Rotas.posicionamento(documentoId)
                        ) {
                            popUpTo(Rotas.NOVO_DOCUMENTO) {
                                inclusive = true
                            }
                        }
                    },

                    onCadastrarPessoa = {
                        navController.navigate(
                            Rotas.CADASTRAR_SIGNATARIO
                        )
                    }
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 5. POSICIONAMENTO DE ASSINATURAS
        composable(
            route = Rotas.POSICIONAMENTO,
            arguments = listOf(
                navArgument("documentoId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val admin = usuarioLogado

            val documentoId =
                entrada.arguments?.getLong("documentoId") ?: -1L

            if (admin?.perfil == Perfil.ADMIN) {

                var autorizado by remember(
                    admin.id,
                    documentoId
                ) {
                    mutableStateOf<Boolean?>(null)
                }

                LaunchedEffect(admin.id, documentoId) {

                    val documento =
                        documentoRepository.buscarPorId(documentoId)

                    autorizado =
                        documento != null &&
                                documento.adminId == admin.id &&
                                documento.status == StatusDocumento.RASCUNHO
                }

                when (autorizado) {

                    null -> {
                        Text("Carregando documento...")
                    }

                    true -> {
                        PosicionamentoScreen(
                            documentoId = documentoId,

                            onContinuar = {
                                navController.navigate(
                                    Rotas.revisaoEnvio(documentoId)
                                )
                            }
                        )
                    }

                    false -> {
                        Text(
                            "Documento não encontrado ou acesso não permitido."
                        )
                    }
                }

            } else {
                RedirecionarParaLogin(navController)
            }
        }


        // 6. REVISÃO E ENVIO
        composable(
            route = Rotas.REVISAO_ENVIO,
            arguments = listOf(
                navArgument("documentoId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val admin = usuarioLogado
            val documentoId =
                entrada.arguments?.getLong("documentoId") ?: -1L

            if (admin?.perfil == Perfil.ADMIN) {

                val scope = rememberCoroutineScope()

                var carregando by remember(documentoId) {
                    mutableStateOf(true)
                }

                var erroCarregamento by remember(documentoId) {
                    mutableStateOf<String?>(null)
                }

                var resumo by remember(documentoId) {
                    mutableStateOf<ResumoDocumentoUi?>(null)
                }

                var participantes by remember(documentoId) {
                    mutableStateOf(
                        emptyList<ParticipanteResumoUi>()
                    )
                }

                var mensagemInicial by remember(documentoId) {
                    mutableStateOf("")
                }

                var enviando by remember(documentoId) {
                    mutableStateOf(false)
                }

                var envioFalhou by remember(documentoId) {
                    mutableStateOf(false)
                }

                // Busca os dados reais salvos no Room
                LaunchedEffect(documentoId, admin.id) {
                    try {
                        val documento =
                            documentoRepository.buscarPorId(documentoId)

                        if (
                            documento == null ||
                            documento.adminId != admin.id ||
                            documento.status != StatusDocumento.RASCUNHO
                        ) {
                            erroCarregamento =
                                "Documento não encontrado ou indisponível."
                        } else {

                            val signatarios =
                                documentoRepository.listarSignatarios(
                                    documentoId
                                )

                            val campos =
                                documentoRepository.listarCampos(
                                    documentoId
                                )

                            val totalPaginas = withContext(
                                Dispatchers.IO
                            ) {
                                RenderizadorPdf(
                                    documento.caminhoPdf
                                ).use {
                                    it.totalPaginas
                                }
                            }

                            resumo = ResumoDocumentoUi(
                                titulo = documento.titulo,
                                totalPaginas = totalPaginas,
                                nomeArquivo = File(
                                    documento.caminhoPdf
                                ).name
                            )

                            participantes = signatarios.map { pessoa ->

                                val camposDaPessoa = campos.filter {
                                    it.documentoSignatarioId ==
                                            pessoa.documentoSignatarioId
                                }

                                val paginas = camposDaPessoa
                                    .map { it.pagina + 1 }
                                    .distinct()
                                    .sorted()

                                ParticipanteResumoUi(
                                    nome = pessoa.nome,
                                    papel = pessoa.papel.rotulo(),
                                    quantidadeDeCampos =
                                        camposDaPessoa.size,
                                    paginas = if (paginas.isEmpty()) {
                                        "Sem campos"
                                    } else {
                                        "Págs. " +
                                                paginas.joinToString(", ")
                                    }
                                )
                            }

                            mensagemInicial =
                                documento.mensagemConvite.ifBlank {
                                    "Você recebeu um documento para assinatura."
                                }
                        }

                    } catch (e: Exception) {
                        erroCarregamento =
                            e.message ?: "Não foi possível carregar o documento."
                    } finally {
                        carregando = false
                    }
                }

                when {
                    carregando -> {
                        Text("Carregando revisão...")
                    }

                    erroCarregamento != null -> {
                        Text(erroCarregamento ?: "")
                    }

                    resumo != null -> {

                        RevisarEnviarScreen(
                            documento = resumo!!,
                            participantes = participantes,
                            mensagemInicial = mensagemInicial,
                            enviando = enviando,
                            envioFalhou = envioFalhou,

                            onVoltar = {
                                navController.popBackStack()
                            },

                            onEditarParticipantes = {
                                navController.popBackStack()
                            },

                            onAjustarCampos = {
                                navController.popBackStack()
                            },

                            onVisualizarPdf = {
                                navController.navigate(
                                    Rotas.visualizarAdmin(documentoId)
                                )
                            },

                            onConfirmarEnvio = { mensagem ->

                                if (!enviando) {
                                    enviando = true
                                    envioFalhou = false

                                    scope.launch {
                                        try {
                                            val atual =
                                                documentoRepository.buscarPorId(
                                                    documentoId
                                                )

                                            if (
                                                atual == null ||
                                                atual.adminId != admin.id ||
                                                atual.status != StatusDocumento.RASCUNHO
                                            ) {
                                                envioFalhou = true
                                            } else {
                                                val resultado =
                                                    documentoRepository.enviar(
                                                        documentoId,
                                                        mensagem
                                                    )


                                                if (resultado.isSuccess) {
                                                    navController.navigate(
                                                        Rotas.documentoEnviado(documentoId)
                                                    ) {
                                                        popUpTo(Rotas.INICIO_ADMIN) {
                                                            inclusive = false
                                                        }
                                                        launchSingleTop = true
                                                    }

                                                } else {
                                                    envioFalhou = true
                                                }
                                            }
                                        } catch (e: Exception) {
                                            envioFalhou = true
                                        } finally {
                                            enviando = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 7. DOCUMENTO ENVIADO
        composable(
            route = Rotas.DOCUMENTO_ENVIADO,
            arguments = listOf(
                navArgument("documentoId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val admin = usuarioLogado
            val documentoId =
                entrada.arguments?.getLong("documentoId") ?: -1L

            if (admin?.perfil == Perfil.ADMIN) {

                var carregando by remember(documentoId) {
                    mutableStateOf(true)
                }

                var erro by remember(documentoId) {
                    mutableStateOf<String?>(null)
                }

                var resumo by remember(documentoId) {
                    mutableStateOf<ResumoDocumentoUi?>(null)
                }

                var participantes by remember(documentoId) {
                    mutableStateOf(
                        emptyList<ParticipanteResumoUi>()
                    )
                }

                var mensagem by remember(documentoId) {
                    mutableStateOf("")
                }

                LaunchedEffect(documentoId, admin.id) {
                    try {
                        val documento =
                            documentoRepository.buscarPorId(documentoId)

                        if (
                            documento == null ||
                            documento.adminId != admin.id ||
                            documento.status == StatusDocumento.RASCUNHO
                        ) {
                            erro =
                                "Documento não enviado ou acesso não permitido."
                        } else {

                            val signatarios =
                                documentoRepository.listarSignatarios(
                                    documentoId
                                )

                            val campos =
                                documentoRepository.listarCampos(
                                    documentoId
                                )

                            val totalPaginas = withContext(
                                Dispatchers.IO
                            ) {
                                RenderizadorPdf(
                                    documento.caminhoPdf
                                ).use {
                                    it.totalPaginas
                                }
                            }

                            resumo = ResumoDocumentoUi(
                                titulo = documento.titulo,
                                totalPaginas = totalPaginas,
                                nomeArquivo = File(
                                    documento.caminhoPdf
                                ).name
                            )

                            participantes = signatarios.map { pessoa ->

                                val camposDaPessoa = campos.filter {
                                    it.documentoSignatarioId ==
                                            pessoa.documentoSignatarioId
                                }

                                val paginas = camposDaPessoa
                                    .map { it.pagina + 1 }
                                    .distinct()
                                    .sorted()

                                ParticipanteResumoUi(
                                    nome = pessoa.nome,
                                    papel = pessoa.papel.rotulo(),
                                    quantidadeDeCampos =
                                        camposDaPessoa.size,
                                    paginas = if (paginas.isEmpty()) {
                                        "Sem campos"
                                    } else {
                                        "Págs. " +
                                                paginas.joinToString(", ")
                                    }
                                )
                            }

                            mensagem = documento.mensagemConvite
                        }

                    } catch (e: Exception) {
                        erro = e.message
                            ?: "Erro ao carregar documento."
                    } finally {
                        carregando = false
                    }
                }

                when {
                    carregando -> {
                        Text("Carregando confirmação...")
                    }

                    erro != null -> {
                        Text(erro ?: "")
                    }

                    resumo != null -> {

                        DocumentoEnviadoScreen(
                            documento = resumo!!,
                            participantes = participantes,
                            mensagemEnviada = mensagem,

                            onVerDetalhes = {
                                navController.navigate(
                                    Rotas.acompanhamento(documentoId)
                                )
                            },

                            onVoltar = {
                                navController.popBackStack()
                            },


                            onVisualizarPdf = {
                                navController.navigate(
                                    Rotas.visualizarAdmin(documentoId)
                                )
                            }
                        )
                    }
                }

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 8. ACOMPANHAMENTO DO DOCUMENTO
        composable(
            route = Rotas.ACOMPANHAMENTO,
            arguments = listOf(
                navArgument("documentoId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val admin = usuarioLogado
            val documentoId =
                entrada.arguments?.getLong("documentoId") ?: -1L

            if (admin?.perfil == Perfil.ADMIN) {

                var autorizado by remember(
                    admin.id,
                    documentoId
                ) {
                    mutableStateOf<Boolean?>(null)
                }

                LaunchedEffect(admin.id, documentoId) {
                    autorizado = try {
                        val documento =
                            documentoRepository.buscarPorId(documentoId)

                        documento != null &&
                                documento.adminId == admin.id

                    } catch (e: Exception) {
                        false
                    }
                }

                when (autorizado) {

                    null -> {
                        Text("Carregando detalhes...")
                    }

                    true -> {
                        DetalhesDocumentoScreen(
                            documentoId = documentoId,
                            documentoRepository = documentoRepository,

                            onVoltarClick = {
                                navController.popBackStack()
                            },


                            onVisualizarDocumentoClick = {
                                navController.navigate(
                                    Rotas.visualizarAdmin(documentoId)
                                )
                            }

                        )
                    }

                    false -> {
                        Text(
                            "Documento não encontrado ou acesso não permitido."
                        )
                    }
                }

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 9. VISUALIZAÇÃO DE PDF PELO ADMIN
        composable(
            route = Rotas.VISUALIZAR_ADMIN,
            arguments = listOf(
                navArgument("documentoId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val admin = usuarioLogado

            val documentoId =
                entrada.arguments?.getLong("documentoId") ?: -1L

            if (admin?.perfil == Perfil.ADMIN) {

                VisualizarDocumentoAdminRoute(
                    documentoId = documentoId,
                    adminId = admin.id,
                    documentoRepository = documentoRepository,

                    onVoltar = {
                        navController.popBackStack()
                    }
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 10. LISTA DE SIGNATÁRIOS
        composable(Rotas.LISTA_SIGNATARIOS) { entrada ->

            val admin = usuarioLogado

            if (admin?.perfil == Perfil.ADMIN) {

                // O formulário avisa pelo savedStateHandle quando salvou, para a lista mostrar "Signatário salvo"
                val avisoSalvo by entrada.savedStateHandle
                    .getStateFlow(CHAVE_SIGNATARIO_SALVO, false)
                    .collectAsState()

                // Rota da Ani: busca os signatários no banco e mostra a tela da Mari
                ListaSignatariosRota(
                    onVoltar = {
                        navController.popBackStack()
                    },

                    onCadastrar = {
                        entrada.savedStateHandle[CHAVE_SIGNATARIO_SALVO] = false
                        navController.navigate(
                            Rotas.CADASTRAR_SIGNATARIO
                        )
                    },

                    onEditar = { signatarioId ->
                        entrada.savedStateHandle[CHAVE_SIGNATARIO_SALVO] = false
                        navController.navigate(
                            Rotas.editarSignatario(signatarioId)
                        )
                    },

                    mostrarAvisoSalvo = avisoSalvo
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 11. CADASTRAR SIGNATÁRIO
        composable(Rotas.CADASTRAR_SIGNATARIO) {

            val admin = usuarioLogado

            if (admin?.perfil == Perfil.ADMIN) {

                // Rota da Ani: sem signatarioId, cadastra uma pessoa nova
                FormularioSignatarioRota(
                    signatarioId = null,

                    onSalvo = {
                        // Avisa a tela anterior (lista ou Novo documento) e volta para ela
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(CHAVE_SIGNATARIO_SALVO, true)
                        navController.popBackStack()
                    },

                    onCancelar = {
                        navController.popBackStack()
                    }
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 12. EDITAR SIGNATÁRIO
        composable(
            route = Rotas.EDITAR_SIGNATARIO,
            arguments = listOf(
                navArgument("signatarioId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val admin = usuarioLogado

            val signatarioId =
                entrada.arguments?.getLong("signatarioId") ?: -1L

            if (admin?.perfil == Perfil.ADMIN) {

                // Rota da Ani: com signatarioId, carrega a pessoa e salva com atualizarSignatario
                FormularioSignatarioRota(
                    signatarioId = signatarioId,

                    onSalvo = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(CHAVE_SIGNATARIO_SALVO, true)
                        navController.popBackStack()
                    },

                    onCancelar = {
                        navController.popBackStack()
                    }
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 13. ASSINAR DOCUMENTO
        composable(
            route = Rotas.ASSINAR_DOCUMENTO,
            arguments = listOf(
                navArgument("documentoId") {
                    type = NavType.LongType
                },
                navArgument("usuarioId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val usuario = usuarioLogado

            val documentoId =
                entrada.arguments?.getLong("documentoId") ?: -1L

            val usuarioId =
                entrada.arguments?.getLong("usuarioId") ?: -1L

            if (
                usuario?.perfil == Perfil.SIGNATARIO &&
                usuario.id == usuarioId
            ) {

                AssinaturaScreen(
                    documentoId = documentoId,
                    usuarioId = usuario.id,

                    onConcluido = {
                        navController.popBackStack()
                    },

                    onVoltar = {
                        navController.popBackStack()
                    }
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 14. VISUALIZAR DOCUMENTO DO SIGNATÁRIO
        composable(
            route = Rotas.VISUALIZAR_DOCUMENTO,
            arguments = listOf(
                navArgument("documentoId") {
                    type = NavType.LongType
                },
                navArgument("usuarioId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val usuario = usuarioLogado

            val documentoId =
                entrada.arguments?.getLong("documentoId") ?: -1L

            val usuarioId =
                entrada.arguments?.getLong("usuarioId") ?: -1L

            if (
                usuario?.perfil == Perfil.SIGNATARIO &&
                usuario.id == usuarioId
            ) {

                VisualizarDocumentoSignatarioScreen(
                    documentoId = documentoId,
                    usuarioId = usuario.id,

                    onVoltar = {
                        navController.popBackStack()
                    }
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }

        // 3. DOCUMENTOS DO SIGNATÁRIO
        composable(
            route = Rotas.DOCUMENTOS_RECEBIDOS,
            arguments = listOf(
                navArgument("usuarioId") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

            val usuario = usuarioLogado
            val idDaRota =
                entrada.arguments?.getLong("usuarioId")

            if (
                usuario?.perfil == Perfil.SIGNATARIO &&
                usuario.id == idDaRota
            ) {
                DocumentosRecebidosScreen(
                    usuarioId = usuario.id,

                    onAssinar = { documentoId ->
                        navController.navigate(
                            Rotas.assinarDocumento(
                                documentoId,
                                usuario.id
                            )
                        )
                    },

                    onVisualizar = { documentoId ->
                        navController.navigate(
                            Rotas.visualizarDocumento(
                                documentoId,
                                usuario.id
                            )
                        )
                    },

                    onSair = sair
                )

            } else {
                RedirecionarParaLogin(navController)
            }
        }
    }
}

// Impede abrir uma área protegida sem login.
@Composable
private fun RedirecionarParaLogin(
    navController: NavHostController
) {
    LaunchedEffect(Unit) {
        navController.navigate(Rotas.LOGIN) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}