package br.com.fiap.letsflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letsflow.data.local.entity.Documento
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
import br.com.fiap.letsflow.data.local.model.DocumentoRecebido
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentoDao {

    @Insert
    suspend fun inserir(documento: Documento): Long

    @Update
    suspend fun atualizar(documento: Documento)

    // Filtra pelo adminId: um Admin só vê os próprios documentos
    @Query("SELECT * FROM documentos WHERE adminId = :adminId ORDER BY dataCriacao DESC")
    fun listarDoAdmin(adminId: Long): Flow<List<Documento>>

    @Query("SELECT * FROM documentos WHERE id = :id")
    suspend fun buscarPorId(id: Long): Documento?

    // Signatário só vê documentos enviados pra ele, nunca rascunhos.
    // Traz junto se ele já assinou, a data do envio e quantos campos são dele
    @Query("""
        SELECT d.id AS documentoId,
               d.titulo AS titulo,
               d.status AS status,
               ds.assinado AS assinado,
               COALESCE(
                   (SELECT MIN(e.dataHora) FROM eventos e WHERE e.documentoId = d.id AND e.tipo = 'ENVIADO'),
                   d.dataCriacao
               ) AS dataRecebimento,
               (SELECT COUNT(*) FROM campos_assinatura c WHERE c.documentoSignatarioId = ds.id) AS totalCampos
        FROM documentos d
        INNER JOIN documento_signatarios ds ON ds.documentoId = d.id
        WHERE ds.usuarioId = :usuarioId AND d.status != 'RASCUNHO'
        ORDER BY dataRecebimento DESC
    """)
    fun listarRecebidos(usuarioId: Long): Flow<List<DocumentoRecebido>>

    @Query("UPDATE documentos SET status = :status WHERE id = :id")
    suspend fun atualizarStatus(id: Long, status: StatusDocumento)

    // Grava onde ficou o PDF final assinado
    @Query("UPDATE documentos SET caminhoPdfFinal = :caminho WHERE id = :id")
    suspend fun salvarPdfFinal(id: Long, caminho: String)
}