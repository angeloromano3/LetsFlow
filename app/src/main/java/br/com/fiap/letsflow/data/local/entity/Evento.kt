package br.com.fiap.letsflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TipoEvento { CRIADO, ENVIADO, ASSINADO, CONCLUIDO }

// Histórico do documento, usado na tela de acompanhamento
@Entity(
    tableName = "eventos",
    foreignKeys = [ForeignKey(
        entity = Documento::class,
        parentColumns = ["id"],
        childColumns = ["documentoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("documentoId")]
)
data class Evento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val tipo: TipoEvento,
    val descricao: String,
    val dataHora: Long = System.currentTimeMillis()
)