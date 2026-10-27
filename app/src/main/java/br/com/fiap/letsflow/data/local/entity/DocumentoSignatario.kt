package br.com.fiap.letsflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Papel { CONTRATANTE, CONTRATADO, TESTEMUNHA }

// Liga um signatário a um documento, com o papel dele e se já assinou
@Entity(
    tableName = "documento_signatarios",
    foreignKeys = [
        ForeignKey(
            entity = Documento::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE  // Se o documento for apagado, os vínculos somem junto
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"]
        )
    ],
    indices = [Index("documentoId"), Index("usuarioId")]
)
data class DocumentoSignatario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val usuarioId: Long,
    val papel: Papel,
    val assinado: Boolean = false,
    val dataAssinatura: Long? = null   // Nulo enquanto não assinou
)