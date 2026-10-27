package br.com.fiap.letsflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fiap.letsflow.data.local.entity.Perfil
import br.com.fiap.letsflow.data.local.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert
    suspend fun inserir(usuario: Usuario): Long

    @Update
    suspend fun atualizar(usuario: Usuario)

    // Login: retorna nulo se e-mail ou senha estiverem errados
    @Query("SELECT * FROM usuarios WHERE email = :email AND senha = :senha LIMIT 1")
    suspend fun login(email: String, senha: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): Usuario?

    // Flow: a lista na tela atualiza sozinha quando alguém é cadastrado
    @Query("SELECT * FROM usuarios WHERE perfil = :perfil ORDER BY nomeCompleto")
    fun listarPorPerfil(perfil: Perfil): Flow<List<Usuario>>
}