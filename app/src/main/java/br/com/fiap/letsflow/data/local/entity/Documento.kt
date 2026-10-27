package br.com.fiap.letsflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class StatusDocumento { RASCUNHO, ENVIADO, PARCIAL, CONCLUIDO }

@Entity(
    tableName = "documentos",
    foreignKeys = [ForeignKey(
        entity = Usuario::class,
        parentColumns = ["id"],
        childColumns = ["adminId"]
    )],
    indices = [Index("adminId")]
)
data class Documento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val caminhoPdf: String,        // O PDF fica salvo no celular; aqui guardamos só o caminho
    val adminId: Long,             // Quem criou: garante que um Admin não veja documentos de outro
    val status: StatusDocumento = StatusDocumento.RASCUNHO,
    val mensagemConvite: String = "",
    val versao: Int = 1,           // Regra de negócio: alterar um documento enviado gera nova versão
    val dataCriacao: Long = System.currentTimeMillis()  // Datas em milissegundos, assim o Room salva sem conversor
)