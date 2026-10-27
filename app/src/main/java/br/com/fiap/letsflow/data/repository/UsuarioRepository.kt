package br.com.fiap.letsflow.data.repository

import br.com.fiap.letsflow.data.local.dao.UsuarioDao
import br.com.fiap.letsflow.data.local.entity.Perfil
import br.com.fiap.letsflow.data.local.entity.Usuario

// As telas só falam com o repositório; na Sprint 2 o DAO é trocado pela API
class UsuarioRepository(private val dao: UsuarioDao) {

    suspend fun login(email: String, senha: String): Usuario? =
        dao.login(email.trim().lowercase(), senha)

    fun listarSignatarios() = dao.listarPorPerfil(Perfil.SIGNATARIO)

    // Valida os dados obrigatórios antes de salvar
    suspend fun cadastrarSignatario(nome: String, email: String, telefone: String, senha: String): Result<Long> {
        val emailLimpo = email.trim().lowercase()
        if (nome.isBlank())
            return Result.failure(Exception("Informe o nome completo"))
        if (!emailLimpo.contains("@"))
            return Result.failure(Exception("E-mail inválido"))
        if (senha.length < 6)
            return Result.failure(Exception("A senha precisa ter pelo menos 6 caracteres"))
        if (dao.buscarPorEmail(emailLimpo) != null)
            return Result.failure(Exception("E-mail já cadastrado"))

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
}