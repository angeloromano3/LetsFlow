package br.com.fiap.letsflow.data.local.storage

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Copia o PDF escolhido pra pasta privada do app, pra ele não sumir se o original for apagado
class ArquivoPdf(private val context: Context) {

    private val pasta = File(context.filesDir, "pdfs").apply { mkdirs() }

    // Retorna o caminho do arquivo salvo, ou erro se o PDF for inválido ou estiver corrompido
    suspend fun salvar(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val destino = File(pasta, "doc_${System.currentTimeMillis()}.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                destino.outputStream().use { saida -> entrada.copyTo(saida) }
            } ?: return@withContext Result.failure(Exception("Não foi possível abrir o arquivo"))

            // Tenta abrir com o PdfRenderer: se falhar, o arquivo não é um PDF válido
            val paginas = ParcelFileDescriptor.open(destino, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { it.pageCount }
            }
            if (paginas == 0) {
                destino.delete()
                return@withContext Result.failure(Exception("O PDF não tem páginas"))
            }
            Result.success(destino.absolutePath)
        } catch (e: Exception) {
            destino.delete()
            Result.failure(Exception("Arquivo inválido ou corrompido"))
        }
    }

    // Usado ao excluir um rascunho, pra não deixar arquivo órfão
    fun apagar(caminho: String) {
        File(caminho).delete()
    }
}