package br.com.fiap.letsflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura

@Dao
interface CampoAssinaturaDao {

    @Insert
    suspend fun inserir(campo: CampoAssinatura): Long

    @Update
    suspend fun atualizar(campo: CampoAssinatura)

    // Salva as posições finais de todos os campos de uma vez, no botão Continuar
    @Update
    suspend fun atualizarTodos(campos: List<CampoAssinatura>)

    @Delete
    suspend fun remover(campo: CampoAssinatura)

    @Query("SELECT * FROM campos_assinatura WHERE documentoId = :documentoId ORDER BY pagina")
    suspend fun listarDoDocumento(documentoId: Long): List<CampoAssinatura>

    // Só os campos de um signatário: é o que ele vê na hora de assinar
    @Query("SELECT * FROM campos_assinatura WHERE documentoSignatarioId = :documentoSignatarioId ORDER BY pagina")
    suspend fun listarDoSignatario(documentoSignatarioId: Long): List<CampoAssinatura>
}