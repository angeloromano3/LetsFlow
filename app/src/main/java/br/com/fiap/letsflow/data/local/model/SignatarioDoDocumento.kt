package br.com.fiap.letsflow.data.local.model

import br.com.fiap.letsflow.data.local.entity.Papel

// Resultado de consulta, não é tabela: o vínculo junto com o nome de quem assina
data class SignatarioDoDocumento(
    val documentoSignatarioId: Long,
    val nome: String,
    val papel: Papel,
    val assinado: Boolean,
    val dataAssinatura: Long?
)