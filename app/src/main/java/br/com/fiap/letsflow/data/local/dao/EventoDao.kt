package br.com.fiap.letsflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import br.com.fiap.letsflow.data.local.entity.Evento
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {

    @Insert
    suspend fun inserir(evento: Evento)

    @Query("SELECT * FROM eventos WHERE documentoId = :documentoId ORDER BY dataHora")
    fun listarDoDocumento(documentoId: Long): Flow<List<Evento>>
}