package br.com.fiap.letsflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "campos_assinatura",
    foreignKeys = [
        ForeignKey(
            entity = Documento::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocumentoSignatario::class,
            parentColumns = ["id"],
            childColumns = ["documentoSignatarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentoId"), Index("documentoSignatarioId")]
)
data class CampoAssinatura(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val documentoSignatarioId: Long,  // Todo campo pertence a um signatário
    val pagina: Int,
    // Posição e tamanho em proporção da página (0.0 a 1.0), não em pixels
    val posicaoX: Float,
    val posicaoY: Float,
    val largura: Float = LARGURA_PADRAO,
    val altura: Float = ALTURA_PADRAO,
    val caminhoAssinatura: String? = null  // Imagem da assinatura salva no celular; nulo enquanto não assinou
) {
    companion object {
        // Tamanho de um campo recém-criado: 30% da largura e 8% da altura da página
        const val LARGURA_PADRAO = 0.30f
        const val ALTURA_PADRAO = 0.08f
    }
}