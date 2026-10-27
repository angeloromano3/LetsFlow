package br.com.fiap.letsflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letsflow.data.local.entity.Documento
import br.com.fiap.letsflow.data.local.entity.StatusDocumento
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

    // Signatário só vê documentos enviados pra ele, e nunca rascunhos
    @Query("""
        SELECT d.* FROM documentos d
        INNER JOIN documento_signatarios ds ON ds.documentoId = d.id
        WHERE ds.usuarioId = :usuarioId AND d.status != 'RASCUNHO'
        ORDER BY d.dataCriacao DESC
    """)
    fun listarRecebidos(usuarioId: Long): Flow<List<Documento>>

    @Query("UPDATE documentos SET status = :status WHERE id = :id")
    suspend fun atualizarStatus(id: Long, status: StatusDocumento)
}