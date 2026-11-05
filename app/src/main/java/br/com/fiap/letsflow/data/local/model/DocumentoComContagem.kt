package br.com.fiap.letsflow.data.local.model

import androidx.room.Embedded
import br.com.fiap.letsflow.data.local.entity.Documento

// Resultado de consulta, não é tabela: o documento junto com a contagem de assinaturas do contador "X de Y"
data class DocumentoComContagem(
    @Embedded val documento: Documento,
    val totalSignatarios: Int, // Y: todos os signatários vinculados ao documento
    val assinados: Int         // X: quantos deles já assinaram
)
