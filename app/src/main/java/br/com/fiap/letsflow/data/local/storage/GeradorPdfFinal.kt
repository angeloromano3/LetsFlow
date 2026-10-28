package br.com.fiap.letsflow.data.local.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import br.com.fiap.letsflow.data.local.entity.CampoAssinatura
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

// Gera o PDF final: cada página do original vira imagem e recebe as assinaturas por cima
object GeradorPdfFinal {

    // Mesma escala da tela: dobro da resolução, pra texto e assinatura ficarem nítidos
    private const val ESCALA = 2f

    suspend fun gerar(
        caminhoOriginal: String,
        campos: List<CampoAssinatura>,
        destino: File
    ): String = withContext(Dispatchers.IO) { // Tudo fora da thread da tela, pra não travar o app
        // Carrega cada PNG uma vez só, mesmo que a assinatura apareça em vários campos
        val assinaturas = mutableMapOf<String, Bitmap?>()
        val pincel = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val pdf = PdfDocument()
        try {
            RenderizadorPdf(caminhoOriginal).use { renderizador ->
                for (indice in 0 until renderizador.totalPaginas) {
                    val imagem = renderizador.renderizarPagina(indice, ESCALA)
                    // Volta pro tamanho original da página, em pontos
                    val largura = (imagem.width / ESCALA).toInt()
                    val altura = (imagem.height / ESCALA).toInt()

                    val pagina = pdf.startPage(
                        PdfDocument.PageInfo.Builder(largura, altura, indice + 1).create()
                    )
                    val canvas = pagina.canvas
                    // Fundo: a página original inteira
                    canvas.drawBitmap(imagem, null, RectF(0f, 0f, largura.toFloat(), altura.toFloat()), pincel)

                    // Por cima: a assinatura de cada campo desta página
                    campos.filter { it.pagina == indice }.forEach { campo ->
                        val caminho = campo.caminhoAssinatura ?: return@forEach
                        val assinatura = assinaturas.getOrPut(caminho) { BitmapFactory.decodeFile(caminho) }
                            ?: return@forEach
                        // Converte a posição relativa (0 a 1) pra pontos desta página
                        val areaCampo = RectF(
                            campo.posicaoX * largura,
                            campo.posicaoY * altura,
                            (campo.posicaoX + campo.largura) * largura,
                            (campo.posicaoY + campo.altura) * altura
                        )
                        canvas.drawBitmap(assinatura, null, encaixar(assinatura, areaCampo), pincel)
                    }

                    pdf.finishPage(pagina)
                    imagem.recycle() // Libera a memória da página antes de ir pra próxima
                }
            }
            destino.parentFile?.mkdirs()
            destino.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
            assinaturas.values.forEach { it?.recycle() }
        }
        destino.absolutePath
    }

    // Igual ao ContentScale.Fit da tela: mantém a proporção da assinatura e centraliza no campo
    private fun encaixar(imagem: Bitmap, area: RectF): RectF {
        val escala = min(area.width() / imagem.width, area.height() / imagem.height)
        val largura = imagem.width * escala
        val altura = imagem.height * escala
        val x = area.left + (area.width() - largura) / 2f
        val y = area.top + (area.height() - altura) / 2f
        return RectF(x, y, x + largura, y + altura)
    }
}