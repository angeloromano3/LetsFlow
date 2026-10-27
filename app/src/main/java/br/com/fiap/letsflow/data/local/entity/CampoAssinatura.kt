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
    val documentoSignatarioId: Long,  // Regra de negócio: todo campo pertence a um signatário
    val pagina: Int,
    // Posição e tamanho em proporção da página (de 0.0 a 1.0), não em pixels.
    // Ex.: x = 0.5 é o meio da largura em qualquer tela. É o requisito técnico do Challenge.
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float
)