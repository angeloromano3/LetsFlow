package br.com.fiap.letsflow.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import java.io.File
import java.io.FileOutputStream

// Transforma os traços desenhados numa imagem PNG salva dentro do app
object ArquivoAssinatura {

    // Proporção 3:1, a mesma do quadro de desenho
    private const val LARGURA = 900
    private const val ALTURA = 300

    fun salvar(context: Context, tracos: List<List<Offset>>, nomeArquivo: String): String {
        // Fundo transparente pra assinatura ficar "por cima" do PDF
        val bitmap = Bitmap.createBitmap(LARGURA, ALTURA, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val pincel = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        // Os traços vêm em proporção (0 a 1), então multiplica pelo tamanho da imagem
        tracos.filter { it.size >= 2 }.forEach { traco ->
            val caminho = Path()
            caminho.moveTo(traco[0].x * LARGURA, traco[0].y * ALTURA)
            traco.drop(1).forEach { caminho.lineTo(it.x * LARGURA, it.y * ALTURA) }
            canvas.drawPath(caminho, pincel)
        }

        val pasta = File(context.filesDir, "assinaturas").apply { mkdirs() }
        val arquivo = File(pasta, nomeArquivo)
        FileOutputStream(arquivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return arquivo.absolutePath
    }
}