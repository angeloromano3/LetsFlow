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
    private val eventoDao: EventoDao
) {

    fun listarDoAdmin(adminId: Long) = documentoDao.listarDoAdmin(adminId)

    fun listarRecebidos(usuarioId: Long) = documentoDao.listarRecebidos(usuarioId)

    fun listarEventos(documentoId: Long) = eventoDao.listarDoDocumento(documentoId)

    suspend fun buscarPorId(documentoId: Long) = documentoDao.buscarPorId(documentoId)

    suspend fun listarSignatarios(documentoId: Long) = documentoSignatarioDao.listarComNome(documentoId)

    suspend fun salvarRascunho(
        titulo: String,
        caminhoPdf: String,
        adminId: Long,
        signatarios: Map<Long, Papel>
    ): Result<Long> {
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

    // ---------- Edição de signatários (só em rascunho) ----------

    // Usuários que ainda podem entrar no documento
    suspend fun listarDisponiveis(documentoId: Long) = documentoSignatarioDao.listarDisponiveis(documentoId)

    // Adiciona um signatário com o papel escolhido
    suspend fun adicionarSignatario(documentoId: Long, usuarioId: Long, papel: Papel): Result<Unit> {
        val documento = documentoDao.buscarPorId(documentoId)
            ?: return Result.failure(Exception("Documento não encontrado"))
        // Depois de enviado, o documento fica preservado
        if (documento.status != StatusDocumento.RASCUNHO)
            return Result.failure(Exception("Documento já enviado não pode ser alterado"))
        documentoSignatarioDao.inserir(
            DocumentoSignatario(documentoId = documentoId, usuarioId = usuarioId, papel = papel)
        )
        return Result.success(Unit)
    }

    // Remove o signatário e todos os campos dele
    suspend fun removerSignatario(documentoId: Long, documentoSignatarioId: Long): Result<Unit> {
        val documento = documentoDao.buscarPorId(documentoId)
            ?: return Result.failure(Exception("Documento não encontrado"))
        if (documento.status != StatusDocumento.RASCUNHO)
            return Result.failure(Exception("Documento já enviado não pode ser alterado"))
        val vinculos = documentoSignatarioDao.listarDoDocumento(documentoId)
        // Regra do Challenge: o documento sempre precisa de pelo menos um signatário
        if (vinculos.size <= 1)
            return Result.failure(Exception("Adicione outro signatário antes de remover o último"))
        val vinculo = vinculos.find { it.id == documentoSignatarioId }
            ?: return Result.failure(Exception("Signatário não encontrado"))
        // Apaga os campos primeiro pra não sobrar campo sem dono
        campoAssinaturaDao.listarDoSignatario(vinculo.id).forEach { campoAssinaturaDao.remover(it) }
        documentoSignatarioDao.remover(vinculo)
        return Result.success(Unit)
    }

    // ---------- Campos de assinatura ----------

    suspend fun listarCampos(documentoId: Long) = campoAssinaturaDao.listarDoDocumento(documentoId)

    suspend fun adicionarCampo(campo: CampoAssinatura): CampoAssinatura {
        val id = campoAssinaturaDao.inserir(campo)
        return campo.copy(id = id)
    }

    suspend fun removerCampo(campo: CampoAssinatura) = campoAssinaturaDao.remover(campo)

    suspend fun salvarPosicoes(campos: List<CampoAssinatura>) = campoAssinaturaDao.atualizarTodos(campos)

    // ---------- Envio e assinatura ----------

    suspend fun enviar(documentoId: Long, mensagemConvite: String = ""): Result<Unit> {
        val documento = documentoDao.buscarPorId(documentoId)
            ?: return Result.failure(Exception("Documento não encontrado"))
        if (documento.status != StatusDocumento.RASCUNHO) return Result.success(Unit)

        val signatarios = documentoSignatarioDao.listarDoDocumento(documentoId)
        if (signatarios.isEmpty()) return Result.failure(Exception("Adicione pelo menos um signatário"))

        val campos = campoAssinaturaDao.listarDoDocumento(documentoId)
        val semCampo = signatarios.filter { s -> campos.none { it.documentoSignatarioId == s.id } }
        if (semCampo.isNotEmpty()) return Result.failure(Exception("Todo signatário precisa de pelo menos um campo"))

        val foraDaPagina = campos.any {
            it.posicaoX < 0f || it.posicaoY < 0f || it.posicaoX + it.largura > 1f || it.posicaoY + it.altura > 1f
        }
        if (foraDaPagina) return Result.failure(Exception("Há campos fora dos limites da página"))

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

    // ---------- Assinatura pelo signatário ----------

    // Vínculo do usuário com o documento; null se ele não for signatário dele
    suspend fun buscarVinculo(documentoId: Long, usuarioId: Long) =
        documentoSignatarioDao.buscarDoUsuario(documentoId, usuarioId)

    // Só os campos de quem está assinando
    suspend fun listarCamposDoSignatario(documentoSignatarioId: Long) =
        campoAssinaturaDao.listarDoSignatario(documentoSignatarioId)

    // Aplica a assinatura em todos os campos do signatário e atualiza o status
    suspend fun assinar(documentoId: Long, usuarioId: Long, caminhoAssinatura: String): Result<Unit> {
        val documento = documentoDao.buscarPorId(documentoId)
            ?: return Result.failure(Exception("Documento não encontrado"))
        // Rascunho ainda não foi enviado; concluído não aceita mais assinatura
        if (documento.status != StatusDocumento.ENVIADO && documento.status != StatusDocumento.PARCIAL) {
            return Result.failure(Exception("Documento não está disponível para assinatura"))
        }
        val vinculo = documentoSignatarioDao.buscarDoUsuario(documentoId, usuarioId)
            ?: return Result.failure(Exception("Você não é signatário deste documento"))
        // Impede assinar duas vezes
        if (vinculo.assinado) return Result.failure(Exception("Você já assinou este documento"))

        campoAssinaturaDao.listarDoSignatario(vinculo.id).forEach {
            campoAssinaturaDao.atualizar(it.copy(caminhoAssinatura = caminhoAssinatura))
        }
        registrarAssinatura(documentoId, vinculo.id)
        return Result.success(Unit)
    }
}