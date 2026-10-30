package br.com.fiap.letsflow.data.local.model

import br.com.fiap.letsflow.data.local.entity.StatusDocumento

// Resultado de consulta, não é tabela: um documento visto pelo signatário, com a situação dele naquele documento
data class DocumentoRecebido(
    val documentoId: Long,
    val titulo: String,
    val status: StatusDocumento, // Situação geral do documento
    val assinado: Boolean,       // Se ESTE signatário já assinou
    val dataRecebimento: Long,   // Data do envio
    val totalCampos: Int         // Campos deste signatário
)