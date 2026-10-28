package br.com.fiap.letsflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.fiap.letsflow.data.local.dao.CampoAssinaturaDao
import br.com.fiap.letsflow.data.local.dao.DocumentoDao
import br.com.fiap.letsflow.data.local.dao.DocumentoSignatarioDao
import br.com.fiap.letsflow.data.local.dao.EventoDao
import br.com.fiap.letsflow.data.local.dao.UsuarioDao
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura
import br.com.fiap.letsflow.data.local.entity.Documento
import br.com.fiap.letsflow.data.local.entity.DocumentoSignatario
import br.com.fiap.letsflow.data.local.entity.Evento
import br.com.fiap.letsflow.data.local.entity.Usuario

@Database(
    entities = [
        Usuario::class,
        Documento::class,
        DocumentoSignatario::class,
        CampoAssinatura::class,
        Evento::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun documentoSignatarioDao(): DocumentoSignatarioDao
    abstract fun campoAssinaturaDao(): CampoAssinaturaDao
    abstract fun eventoDao(): EventoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "letsflow.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(usuariosIniciais) // Insere os usuários de teste quando o banco é criado
                    .build()
                    .also { instancia = it }
            }

        // Roda só uma vez, na criação do banco: um Admin e um Signatário pra testar o login
        private val usuariosIniciais = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    """
                    INSERT INTO usuarios (nomeCompleto, email, telefone, senha, perfil) VALUES
                    ('Administrador', 'admin@letsflow.com', '11999990000', '123456', 'ADMIN'),
                    ('Maria Souza', 'maria@letsflow.com', '11988887777', '123456', 'SIGNATARIO'),
                    ('João Lima', 'joao@letsflow.com', '11977776666', '123456', 'SIGNATARIO') -- Segundo signatário, pra testar o status parcial
                    """.trimIndent()
                )
            }
        }
    }
}