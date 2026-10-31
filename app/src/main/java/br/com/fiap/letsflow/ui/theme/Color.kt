package br.com.fiap.letsflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Cada papel de cor do Figma; o tema escolhe a versão clara ou escura
@Immutable
data class PaletaLetsFlow(
    val acao: Color,             // Botões, links e abas ativas
    val textoSobreAcao: Color,   // Texto em cima do azul de ação
    val fundo: Color,            // Fundo das telas
    val superficie: Color,       // Cartões e diálogos
    val destaque: Color,         // Azul claro: realce e etiqueta "Aguardando"
    val texto: Color,            // Texto principal
    val textoSecundario: Color,  // Legendas e metadados
    val divisao: Color,          // Bordas e linhas
    val acento: Color,           // Terracota
    val pendente: Color,         // Em andamento / pendente
    val fundoPendente: Color,
    val concluido: Color,        // Concluído / sucesso
    val fundoConcluido: Color,
    val erro: Color,             // Erro / bloqueio
    val fundoErro: Color,
    val fundoDesabilitado: Color,
    val textoDesabilitado: Color
)

// Paleta oficial do Figma (os tons de estado foram estimados pela imagem)
val PaletaClara = PaletaLetsFlow(
    acao = Color(0xFF244B70),
    textoSobreAcao = Color.White,
    fundo = Color(0xFFFAFBFD),
    superficie = Color.White,
    destaque = Color(0xFFEDF3F8),
    texto = Color(0xFF1D3044),
    textoSecundario = Color(0xFF64748B),
    divisao = Color(0xFFDDE4EB),
    acento = Color(0xFFBD8059),
    pendente = Color(0xFFA66A1C),
    fundoPendente = Color(0xFFFEF1E2),
    concluido = Color(0xFF1F7A55),
    fundoConcluido = Color(0xFFE5F3EB),
    erro = Color(0xFFB4444A),
    fundoErro = Color(0xFFFCECEC),
    fundoDesabilitado = Color(0xFFE3E8EF),
    textoDesabilitado = Color(0xFF64748B)
)

// Versão escura derivada do Figma
val PaletaEscura = PaletaLetsFlow(
    acao = Color(0xFF8FB4DA),
    textoSobreAcao = Color(0xFF0F2438),
    fundo = Color(0xFF121A24),
    superficie = Color(0xFF1A2633),
    destaque = Color(0xFF243444),
    texto = Color(0xFFE6EDF4),
    textoSecundario = Color(0xFF9AA9BA),
    divisao = Color(0xFF2E3D4D),
    acento = Color(0xFFE0A47D),
    pendente = Color(0xFFE8B66A),
    fundoPendente = Color(0xFF3A2C14),
    concluido = Color(0xFF7CCBA2),
    fundoConcluido = Color(0xFF163A2A),
    erro = Color(0xFFF2A3A6),
    fundoErro = Color(0xFF5C2025),
    fundoDesabilitado = Color(0xFF243444),
    textoDesabilitado = Color(0xFF6B7B8C)
)

// Cores que identificam cada signatário nos campos; ficam sobre o papel branco, então não mudam no modo escuro
val CoresSignatarios = listOf(
    Color(0xFF1E88E5), // Azul
    Color(0xFFE53935), // Vermelho
    Color(0xFF43A047), // Verde
    Color(0xFFFB8C00), // Laranja
    Color(0xFF8E24AA)  // Roxo
)

// Cor da pessoa pela posição dela na lista; recomeça do início se houver mais pessoas que cores
fun corDoSignatario(indice: Int) = CoresSignatarios[indice % CoresSignatarios.size]