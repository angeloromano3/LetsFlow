package br.com.fiap.letsflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import br.com.fiap.letsflow.data.local.entity.DocumentoSignatario

@Dao
interface DocumentoSignatarioDao {

    @Insert
    suspend fun inserir(vinculo: DocumentoSignatario): Long

    // Permite remover um signatário antes do envio, como o PDF pede
    @Delete
    suspend fun remover(vinculo: DocumentoSignatario)

    @Query("SELECT * FROM documento_signatarios WHERE documentoId = :documentoId")
    suspend fun listarDoDocumento(documentoId: Long): List<DocumentoSignatario>

    @Query("UPDATE documento_signatarios SET assinado = 1, dataAssinatura = :data WHERE id = :id")
    suspend fun marcarAssinado(id: Long, data: Long = System.currentTimeMillis())

    // Usado pra saber se o documento já pode virar CONCLUIDO
    @Query("SELECT COUNT(*) FROM documento_signatarios WHERE documentoId = :documentoId AND assinado = 0")
    suspend fun contarPendentes(documentoId: Long): Int
}