package br.com.fiap.letsflow.data.repository

import br.com.fiap.letsflow.data.local.dao.CampoAssinaturaDao
import br.com.fiap.letsflow.data.local.dao.DocumentoDao
import br.com.fiap.letsflow.data.local.dao.DocumentoSignatarioDao
import br.com.fiap.letsflow.data.local.dao.EventoDao
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura
import br.com.fiap.letsflow.data.local.entity.Documento
import br.com.fiap.letsflow.data.local.entity.DocumentoSignatario
import br.com.fiap.letsflow.data.local.entity.Evento
import br.com.fiap.letsflow.data.local.entity.Papel
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.data.local.entity.TipoEvento

class DocumentoRepository(
    private val documentoDao: DocumentoDao,
    private val documentoSignatarioDao: DocumentoSignatarioDao,
    private val campoAssinaturaDao: CampoAssinaturaDao,
    private val eventoDao: EventoDao // Grava o histórico usado na tela de acompanhamento
) {

    fun listarDoAdmin(adminId: Long) = documentoDao.listarDoAdmin(adminId)

    fun listarRecebidos(usuarioId: Long) = documentoDao.listarRecebidos(usuarioId)

    fun listarEventos(documentoId: Long) = eventoDao.listarDoDocumento(documentoId)

    suspend fun buscarPorId(documentoId: Long) = documentoDao.buscarPorId(documentoId)

    suspend fun listarSignatarios(documentoId: Long) = documentoSignatarioDao.listarComNome(documentoId)

    // Cria o documento como rascunho; signatarios é um mapa de id do usuário para o papel dele
    suspend fun salvarRascunho(
        titulo: String,
        caminhoPdf: String,
        adminId: Long,
        signatarios: Map<Long, Papel>
    ): Result<Long> {
        // Valida os dados obrigatórios antes de salvar
        if (titulo.isBlank()) return Result.failure(Exception("Informe o título do documento"))
        if (signatarios.isEmpty()) return Result.failure(Exception("Adicione pelo menos um signatário"))

        val documentoId = documentoDao.inserir(
            Documento(
                titulo = titulo.trim(),
                caminhoPdf = caminhoPdf,
                adminId = adminId,
                status = StatusDocumento.RASCUNHO
            )
        )

        // Cria um vínculo para cada signatário escolhido, com o papel dele
        signatarios.forEach { (usuarioId, papel) ->
            documentoSignatarioDao.inserir(
                DocumentoSignatario(
                    documentoId = documentoId,
                    usuarioId = usuarioId,
                    papel = papel
                )
            )
        }

        eventoDao.inserir(Evento(documentoId = documentoId, tipo = TipoEvento.CRIADO, descricao = "Documento criado"))
        return Result.success(documentoId)
    }

    // ---------- Campos de assinatura ----------

    suspend fun listarCampos(documentoId: Long) = campoAssinaturaDao.listarDoDocumento(documentoId)

    // Salva na hora e devolve o campo já com o id gerado pelo Room
    suspend fun adicionarCampo(campo: CampoAssinatura): CampoAssinatura {
        val id = campoAssinaturaDao.inserir(campo)
        return campo.copy(id = id)
    }

    suspend fun removerCampo(campo: CampoAssinatura) = campoAssinaturaDao.remover(campo)

    // Grava as posições finais depois que o Admin terminou de mover e redimensionar
    suspend fun salvarPosicoes(campos: List<CampoAssinatura>) = campoAssinaturaDao.atualizarTodos(campos)

    // ---------- Envio e assinatura ----------

    // Regras do PDF checadas antes do envio
    suspend fun enviar(documentoId: Long, mensagemConvite: String = ""): Result<Unit> {
        val documento = documentoDao.buscarPorId(documentoId)
            ?: return Result.failure(Exception("Documento não encontrado"))

        // Se já foi enviado, não envia de novo: evita duplicar as solicitações numa nova tentativa
        if (documento.status != StatusDocumento.RASCUNHO) return Result.success(Unit)

        val signatarios = documentoSignatarioDao.listarDoDocumento(documentoId)
        if (signatarios.isEmpty()) return Result.failure(Exception("Adicione pelo menos um signatário"))

        val campos = campoAssinaturaDao.listarDoDocumento(documentoId)
        val semCampo = signatarios.filter { s -> campos.none { it.documentoSignatarioId == s.id } }
        if (semCampo.isNotEmpty()) return Result.failure(Exception("Todo signatário precisa de pelo menos um campo"))

        val foraDaPagina = campos.any { it.posicaoX < 0f || it.posicaoY < 0f || it.posicaoX + it.largura > 1f || it.posicaoY + it.altura > 1f }
        if (foraDaPagina) return Result.failure(Exception("Há campos fora dos limites da página"))

        // Salva a mensagem do convite junto com a mudança de status
        documentoDao.atualizar(
            documento.copy(status = StatusDocumento.ENVIADO, mensagemConvite = mensagemConvite.trim())
        )
        eventoDao.inserir(
            Evento(
                documentoId = documentoId,
                tipo = TipoEvento.ENVIADO,
                descricao = "Enviado para ${signatarios.size} signatário(s)"
            )
        )
        return Result.success(Unit)
    }

    // Só vira CONCLUIDO quando ninguém mais está pendente
    suspend fun registrarAssinatura(documentoId: Long, documentoSignatarioId: Long) {
        documentoSignatarioDao.marcarAssinado(documentoSignatarioId)
        eventoDao.inserir(Evento(documentoId = documentoId, tipo = TipoEvento.ASSINADO, descricao = "Assinatura registrada"))

        val pendentes = documentoSignatarioDao.contarPendentes(documentoId)
        if (pendentes == 0) {
            documentoDao.atualizarStatus(documentoId, StatusDocumento.CONCLUIDO)
            eventoDao.inserir(Evento(documentoId = documentoId, tipo = TipoEvento.CONCLUIDO, descricao = "Todos assinaram"))
        } else {
            documentoDao.atualizarStatus(documentoId, StatusDocumento.PARCIAL)
        }
    }
}