package br.com.fiap.letsflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// Guarda a paleta atual pra qualquer tela ler
private val LocalPaleta = staticCompositionLocalOf { PaletaClara }

// Atalho: LetsFlowTheme.cores.acao, LetsFlowTheme.cores.texto...
object LetsFlowTheme {
    val cores: PaletaLetsFlow
        @Composable @ReadOnlyComposable
        get() = LocalPaleta.current
}

// Liga a paleta aos papéis do Material, pra botões, campos e diálogos padrão já saírem certos
private fun esquemaMaterial(p: PaletaLetsFlow, escuro: Boolean): ColorScheme {
    val base = if (escuro) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.acao,
        onPrimary = p.textoSobreAcao,
        primaryContainer = p.destaque,
        onPrimaryContainer = p.acao,
        secondary = p.texto,
        secondaryContainer = p.destaque, // Fundo dos chips selecionados
        onSecondaryContainer = p.acao,
        tertiary = p.acento,
        background = p.fundo,
        onBackground = p.texto,
        surface = p.superficie,
        onSurface = p.texto,
        surfaceVariant = p.destaque,
        onSurfaceVariant = p.textoSecundario,
        surfaceTint = p.acao,
        // Sem isso, cartões e diálogos ganham o tom lilás padrão do Material 3
        surfaceContainerLowest = p.superficie,
        surfaceContainerLow = p.fundo,
        surfaceContainer = p.fundo,
        surfaceContainerHigh = p.superficie,
        surfaceContainerHighest = p.destaque,
        outline = p.divisao,
        outlineVariant = p.divisao,
        error = p.erro,
        errorContainer = p.fundoErro,
        onErrorContainer = p.erro
    )
}

// Segue o modo do sistema; a cor dinâmica fica desligada pra manter a identidade
@Composable
fun LetsFlowTheme(
    escuro: Boolean = isSystemInDarkTheme(), // Dá pra forçar passando true ou false
    content: @Composable () -> Unit
) {
    val paleta = if (escuro) PaletaEscura else PaletaClara
    CompositionLocalProvider(LocalPaleta provides paleta) {
        MaterialTheme(
            colorScheme = esquemaMaterial(paleta, escuro),
            typography = Typography,
            content = content
        )
    }
}