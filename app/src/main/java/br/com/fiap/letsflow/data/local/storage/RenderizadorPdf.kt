package br.com.fiap.letsflow.data.local.storage

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

// Abre o PDF já salvo pelo ArquivoPdf e transforma cada página numa imagem pra mostrar na tela
class RenderizadorPdf(caminho: String) : Closeable {

    private val descritor = ParcelFileDescriptor.open(File(caminho), ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descritor)

    // O PdfRenderer só deixa uma página aberta por vez; o Mutex faz as chamadas esperarem a vez
    private val trava = Mutex()

    val totalPaginas: Int get() = renderer.pageCount

    // escala 2 = imagem com o dobro da resolução da página, pra ficar nítida na tela
    suspend fun renderizarPagina(indice: Int, escala: Float = 2f): Bitmap = trava.withLock {
        withContext(Dispatchers.IO) {
            renderer.openPage(indice).use { pagina ->
                val imagem = Bitmap.createBitmap(
                    (pagina.width * escala).toInt(),
                    (pagina.height * escala).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                imagem.eraseColor(Color.WHITE)  // PDF sem fundo apareceria transparente
                pagina.render(imagem, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                imagem
            }
        }
    }

    // Libera o arquivo quando a tela fechar
    override fun close() {
        renderer.close()
        descritor.close()
    }
}