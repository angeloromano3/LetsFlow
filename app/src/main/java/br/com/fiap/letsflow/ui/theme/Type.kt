package br.com.fiap.letsflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.fiap.letsflow.R

// DM Sans com os 4 pesos baixados
val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold)
)

private val padrao = Typography()

// Todos os estilos usam DM Sans; os do Figma têm tamanho e peso próprios
val Typography = Typography(
    displayLarge = padrao.displayLarge.copy(fontFamily = DmSans),
    displayMedium = padrao.displayMedium.copy(fontFamily = DmSans),
    displaySmall = padrao.displaySmall.copy(fontFamily = DmSans),
    headlineLarge = padrao.headlineLarge.copy(fontFamily = DmSans),
    // Título de tela · 26 / Bold
    headlineMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = padrao.headlineSmall.copy(fontFamily = DmSans),
    // Título de seção · 19 / SemiBold
    titleLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = padrao.titleMedium.copy(fontFamily = DmSans, fontWeight = FontWeight.SemiBold),
    titleSmall = padrao.titleSmall.copy(fontFamily = DmSans),
    // Texto de interface · 16 / Regular
    bodyLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    // Metadados e legendas · 14 e 13 / Regular
    bodyMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    // Texto dos botões
    labelLarge = padrao.labelLarge.copy(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = padrao.labelMedium.copy(fontFamily = DmSans),
    labelSmall = padrao.labelSmall.copy(fontFamily = DmSans)
)