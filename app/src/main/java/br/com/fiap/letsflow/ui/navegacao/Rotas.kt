package br.com.fiap.letsflow.ui.navegacao

object Rotas {

    // AUTENTICAÇÃO
    const val LOGIN = "login"

    // ADMIN
    const val INICIO_ADMIN = "admin/inicio"
    const val NOVO_DOCUMENTO = "admin/documentos/novo"

    const val POSICIONAMENTO =
        "documentos/{documentoId}/posicionamento"

    const val REVISAO_ENVIO =
        "documentos/{documentoId}/revisao"

    const val DOCUMENTO_ENVIADO =
        "documentos/{documentoId}/enviado"

    const val ACOMPANHAMENTO =
        "documentos/{documentoId}/detalhes"

    const val VISUALIZAR_ADMIN =
        "admin/documentos/{documentoId}/visualizar"

    // CADASTRO E GERENCIAMENTO DE SIGNATÁRIOS
    const val LISTA_SIGNATARIOS = "admin/signatarios"
    const val CADASTRAR_SIGNATARIO = "admin/signatarios/novo"

    const val EDITAR_SIGNATARIO =
        "admin/signatarios/{signatarioId}/editar"

    // SIGNATÁRIO
    const val DOCUMENTOS_RECEBIDOS =
        "signatario/{usuarioId}/documentos"

    const val ASSINAR_DOCUMENTO =
        "signatario/{usuarioId}/documentos/{documentoId}/assinar"

    const val VISUALIZAR_DOCUMENTO =
        "signatario/{usuarioId}/documentos/{documentoId}/visualizar"

    // CONSTRUÇÃO DE ROTAS COM IDENTIFICADORES

    fun posicionamento(documentoId: Long) =
        "documentos/$documentoId/posicionamento"

    fun revisaoEnvio(documentoId: Long) =
        "documentos/$documentoId/revisao"

    fun documentoEnviado(documentoId: Long) =
        "documentos/$documentoId/enviado"

    fun acompanhamento(documentoId: Long) =
        "documentos/$documentoId/detalhes"

    fun visualizarAdmin(documentoId: Long) =
        "admin/documentos/$documentoId/visualizar"

    fun editarSignatario(signatarioId: Long) =
        "admin/signatarios/$signatarioId/editar"

    fun documentosRecebidos(usuarioId: Long) =
        "signatario/$usuarioId/documentos"

    fun assinarDocumento(
        documentoId: Long,
        usuarioId: Long
    ) = "signatario/$usuarioId/documentos/$documentoId/assinar"

    fun visualizarDocumento(
        documentoId: Long,
        usuarioId: Long
    ) = "signatario/$usuarioId/documentos/$documentoId/visualizar"
}
