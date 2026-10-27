package br.com.fiap.letsflow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Perfil { ADMIN, SIGNATARIO }

// E-mail único, porque ele é a credencial de login
@Entity(tableName = "usuarios", indices = [Index(value = ["email"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nomeCompleto: String,
    val email: String,
    val telefone: String,
    val senha: String,
    val perfil: Perfil
)