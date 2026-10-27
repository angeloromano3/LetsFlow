# Let's Flow

Aplicativo Android para preparar, enviar e assinar documentos PDF pelo celular.
Projeto do Challenge FIAP 2TDSOS em parceria com a LetsSign (Sprint 1).

## Como funciona

**Admin**
1. Faz login e cadastra os signatários.
2. Cria um documento: informa o título, escolhe o PDF e seleciona os signatários com o papel de cada um (contratante, contratado ou testemunha).
3. Posiciona os campos de assinatura nas páginas do PDF.
4. Revisa e envia.
5. Acompanha o status de cada signatário e o histórico do documento.

**Signatário**
1. Faz login e vê os documentos pendentes e os já assinados.
2. Abre o documento e assina nos campos dele.
3. Quando todos assinam, o documento passa a ser concluído.

## Tecnologias
- Kotlin + Jetpack Compose (Material 3)
- Room (banco local)
- Navigation Compose

## Estrutura
- `data/local/entity`: tabelas do banco (Usuario, Documento, DocumentoSignatario, CampoAssinatura, Evento)
- `data/local/dao`: consultas de cada tabela
- `data/local/AppDatabase`: configuração do banco

Os campos de assinatura são salvos em proporção da página (0 a 1), e não em pixels, para manter a posição em qualquer tela.

## Como rodar
1. Clone o repositório e abra no Android Studio.
2. Aguarde o Gradle sincronizar.
3. Rode em um emulador ou celular com Android 8.0 (API 26) ou superior.

## Logins de teste
- Admin: `admin@letsflow.com` / `123456`
- Signatário: `maria@letsflow.com` / `123456`

## Equipe
- Angelo Romano
- Amanda Uechi
- Mariana Malvão
- Grace camara
- aniole