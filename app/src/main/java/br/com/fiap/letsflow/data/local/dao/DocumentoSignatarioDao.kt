package br.com.fiap.letsflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import br.com.fiap.letsflow.data.local.entity.DocumentoSignatario
import br.com.fiap.letsflow.data.local.model.SignatarioDoDocumento

@Dao
interface DocumentoSignatarioDao {

    @Insert
    suspend fun inserir(vinculo: DocumentoSignatario): Long

    @Delete
    suspend fun remover(vinculo: DocumentoSignatario)

    @Query("SELECT * FROM documento_signatarios WHERE documentoId = :documentoId")
    suspend fun listarDoDocumento(documentoId: Long): List<DocumentoSignatario>

    @Query("""
        SELECT ds.id AS documentoSignatarioId, u.nomeCompleto AS nome,
               ds.papel AS papel, ds.assinado AS assinado, ds.dataAssinatura AS dataAssinatura
        FROM documento_signatarios ds
        INNER JOIN usuarios u ON u.id = ds.usuarioId
        WHERE ds.documentoId = :documentoId
        ORDER BY ds.id
    """)
    suspend fun listarComNome(documentoId: Long): List<SignatarioDoDocumento>

    // Vínculo do usuário logado com o documento; volta null se ele não for signatário
    @Query("SELECT * FROM documento_signatarios WHERE documentoId = :documentoId AND usuarioId = :usuarioId LIMIT 1")
    suspend fun buscarDoUsuario(documentoId: Long, usuarioId: Long): DocumentoSignatario?

    @Query("UPDATE documento_signatarios SET assinado = 1, dataAssinatura = :data WHERE id = :id")
    suspend fun marcarAssinado(id: Long, data: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM documento_signatarios WHERE documentoId = :documentoId AND assinado = 0")
    suspend fun contarPendentes(documentoId: Long): Int
}