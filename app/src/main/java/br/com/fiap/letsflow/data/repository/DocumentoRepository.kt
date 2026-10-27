package br.com.fiap.letsflow.data.repository

import br.com.fiap.letsflow.data.local.dao.CampoAssinaturaDao
import br.com.fiap.letsflow.data.local.dao.DocumentoDao
import br.com.fiap.letsflow.data.local.dao.DocumentoSignatarioDao
import br.com.fiap.letsflow.data.local.entity.StatusDocumento

class DocumentoRepository(
    private val documentoDao: DocumentoDao,
    private val documentoSignatarioDao: DocumentoSignatarioDao,
    private val campoAssinaturaDao: CampoAssinaturaDao
) {

    fun listarDoAdmin(adminId: Long) = documentoDao.listarDoAdmin(adminId)

    fun listarRecebidos(usuarioId: Long) = documentoDao.listarRecebidos(usuarioId)

    // Regras do PDF checadas antes do envio
    suspend fun enviar(documentoId: Long): Result<Unit> {
        val signatarios = documentoSignatarioDao.listarDoDocumento(documentoId)
        if (signatarios.isEmpty()) return Result.failure(Exception("Adicione pelo menos um signatário"))

        val campos = campoAssinaturaDao.listarDoDocumento(documentoId)
        val semCampo = signatarios.filter { s -> campos.none { it.documentoSignatarioId == s.id } }
        if (semCampo.isNotEmpty()) return Result.failure(Exception("Todo signatário precisa de pelo menos um campo"))

        val foraDaPagina = campos.any { it.x < 0f || it.y < 0f || it.x + it.largura > 1f || it.y + it.altura > 1f }
        if (foraDaPagina) return Result.failure(Exception("Há campos fora dos limites da página"))

        documentoDao.atualizarStatus(documentoId, StatusDocumento.ENVIADO)
        return Result.success(Unit)
    }

    // Só vira CONCLUIDO quando ninguém mais está pendente
    suspend fun registrarAssinatura(documentoId: Long, documentoSignatarioId: Long) {
        documentoSignatarioDao.marcarAssinado(documentoSignatarioId)
        val pendentes = documentoSignatarioDao.contarPendentes(documentoId)
        val novoStatus = if (pendentes == 0) StatusDocumento.CONCLUIDO else StatusDocumento.PARCIAL
        documentoDao.atualizarStatus(documentoId, novoStatus)
    }
}