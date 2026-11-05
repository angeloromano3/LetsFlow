package br.com.fiap.letsflow.data.repository

import br.com.fiap.letsflow.data.local.dao.UsuarioDao
import br.com.fiap.letsflow.data.local.entity.Perfil
import br.com.fiap.letsflow.data.local.entity.Usuario

// As telas só falam com o repositório; na Sprint 2 o DAO é trocado pela API
class UsuarioRepository(private val dao: UsuarioDao) {

    suspend fun login(email: String, senha: String): Usuario? =
        dao.login(email.trim().lowercase(), senha)

    suspend fun buscarPorId(id: Long): Usuario? = dao.buscarPorId(id)

    fun listarSignatarios() = dao.listarPorPerfil(Perfil.SIGNATARIO)

    // Valida os dados obrigatórios antes de salvar
    suspend fun cadastrarSignatario(nome: String, email: String, telefone: String, senha: String): Result<Long> {
        val emailLimpo = email.trim().lowercase()
        if (nome.isBlank()) return Result.failure(Exception("Informe o nome completo"))
        if (!emailLimpo.contains("@")) return Result.failure(Exception("E-mail inválido"))
        if (senha.length < 6) return Result.failure(Exception("A senha precisa ter pelo menos 6 caracteres"))
        if (dao.buscarPorEmail(emailLimpo) != null) return Result.failure(Exception("E-mail já cadastrado"))

        val id = dao.inserir(
            Usuario(
                nomeCompleto = nome.trim(),
                email = emailLimpo,
                telefone = telefone.trim(),
                senha = senha,
                perfil = Perfil.SIGNATARIO
            )
        )
        return Result.success(id)
    }

    // Edita os dados de um signatário existente
    suspend fun editarSignatario(
        id: Long,
        nome: String,
        email: String,
        telefone: String,
        senha: String
    ): Result<Unit> {

        return try {
            val usuario = dao.buscarPorId(id)

            if (
                usuario == null ||
                usuario.perfil != Perfil.SIGNATARIO
            ) {
                return Result.failure(
                    Exception("Signatário não encontrado")
                )
            }

            val nomeLimpo = nome.trim()
            val emailLimpo = email.trim().lowercase()
            val telefoneLimpo = telefone.trim()

            if (nomeLimpo.isBlank()) {
                return Result.failure(
                    Exception("Informe o nome completo")
                )
            }

            val emailValido = Regex(
                "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"
            ).matches(emailLimpo)

            if (!emailValido) {
                return Result.failure(
                    Exception("E-mail inválido")
                )
            }

            if (telefoneLimpo.count { it.isDigit() } < 10) {
                return Result.failure(
                    Exception("Telefone inválido")
                )
            }

            if (senha.isNotEmpty() && senha.length < 6) {
                return Result.failure(
                    Exception("A senha precisa ter pelo menos 6 caracteres")
                )
            }

            val outroUsuario = dao.buscarPorEmail(emailLimpo)

            if (
                outroUsuario != null &&
                outroUsuario.id != id
            ) {
                return Result.failure(
                    Exception("E-mail já cadastrado")
                )
            }

            val usuarioAtualizado = usuario.copy(
                nomeCompleto = nomeLimpo,
                email = emailLimpo,
                telefone = telefoneLimpo,
                senha = if (senha.isBlank()) {
                    usuario.senha
                } else {
                    senha
                }
            )

            dao.atualizar(usuarioAtualizado)

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}