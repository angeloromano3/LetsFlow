# Let's Flow

Aplicativo Android nativo para preparar, enviar e assinar documentos PDF pelo celular, com dois perfis: **Admin** e **Signatário**.

Projeto do **Challenge FIAP 2TDSOS** em parceria com a **LetsSign** — Sprint 1.

> Na Sprint 1 o app funciona 100% local: os dados ficam no banco Room do próprio celular. Envio por e-mail, notificações, deeplinks e backend (Spring Boot + MongoDB) fazem parte da Sprint 2.

---

## Funcionalidades

### Admin
- **Login** com e-mail e senha.
- **Documentos** (tela inicial): lista com as abas Todos, Em andamento e Concluídos, mostrando quantas pessoas já assinaram cada documento.
- **Signatários**: cadastro e edição de quem vai assinar (nome, e-mail, telefone e senha), com busca.
- **Novo documento** (Etapa 1 de 3): título, PDF escolhido do celular (o app avisa se o arquivo for inválido) e seleção das pessoas com o papel de cada uma — contratante, contratado ou testemunha.
- **Posicionar assinaturas** (Etapa 2 de 3): toque na página para criar o campo, arraste para mover, use pinça para redimensionar e segure para apagar. Botões de zoom (+ e −) para documentos pequenos.
- **Revisar e enviar** (Etapa 3 de 3): resumo do documento, das pessoas e dos campos, com mensagem de convite.
- **Detalhes do documento**: status, contador de assinaturas, situação de cada participante e histórico com data e hora.

### Signatário
- **Login** com o e-mail e a senha cadastrados pelo Admin.
- **Meus documentos**: abas Pendentes e Assinados, com contador de pendências.
- **Visualizar e assinar**: a pessoa desenha a assinatura uma vez, confere a prévia nos campos e pode refazer antes de confirmar. A assinatura é aplicada em todos os campos dela.
- **Visualizar documento** em modo somente leitura.
- Quando todos assinam, o documento passa para **Concluído** e o app gera o **PDF final** com todas as assinaturas.

---

## Decisões técnicas

- **Posição relativa dos campos:** página, posição e tamanho de cada campo são salvos em proporção da página (valores de 0 a 1), e não em pixels. Assim o campo fica no mesmo lugar em qualquer tamanho de tela.
- **PDF no sistema de arquivos:** o PDF é copiado para a pasta interna do app, e o banco guarda só o caminho do arquivo.
- **Repositórios:** as telas falam apenas com os repositórios, nunca direto com o banco. Na Sprint 2, basta trocar o DAO do Room por chamadas à API REST, sem mudar as telas.
- **Regras de negócio:** documento precisa de pelo menos 1 signatário; signatários só podem ser alterados antes do envio; o signatário só vê os documentos enviados para ele; o status vira Parcial após a primeira assinatura e Concluído quando todos assinam.
- **Histórico:** criação, envio e cada assinatura são registrados como eventos com data e hora.
- **Identidade visual:** cores e fonte (DM Sans) do protótipo no Figma, com modo escuro seguindo o sistema. A folha do PDF fica sempre branca, como papel.

---

## Tecnologias

| | |
|---|---|
| Linguagem | Kotlin 2.2 |
| Interface | Jetpack Compose + Material 3 |
| Banco local | Room 2.8 (KSP) |
| Navegação | Navigation Compose 2.10 |
| PDF | `PdfRenderer` (leitura) e `PdfDocument` (PDF final assinado), nativos do Android |
| Android mínimo | 8.0 (API 26) |

---

## Estrutura do projeto

```
app/src/main/java/br/com/fiap/letsflow/
├── data/
│   ├── local/
│   │   ├── entity/      # Tabelas: Usuario, Documento, DocumentoSignatario, CampoAssinatura, Evento
│   │   ├── model/       # Resultados de consultas com JOIN
│   │   ├── dao/         # Consultas de cada tabela
│   │   ├── storage/     # Arquivos: PDF, imagem da assinatura e geração do PDF final
│   │   └── AppDatabase.kt
│   └── repository/      # DocumentoRepository e UsuarioRepository (regras de negócio)
└── ui/
    ├── login/           # Login
    ├── documento/       # Documentos, Novo documento, Posicionar, Revisar e enviar, Detalhes
    ├── signatario/      # Lista e formulário de signatários
    ├── assinatura/      # Meus documentos, Visualizar e assinar, Visualizar documento
    ├── componentes/     # Botões, etiquetas de status e controles de zoom
    └── theme/           # Cores, tipografia e tema (claro e escuro)
```

---

## Como rodar

1. Clone o repositório e abra a pasta no **Android Studio**.
2. Aguarde o Gradle sincronizar.
3. Rode em um emulador ou celular com **Android 8.0 (API 26) ou superior**.

Ou instale direto o **APK** enviado junto com a entrega.

### Logins de teste

Os usuários abaixo são criados automaticamente na primeira vez que o app abre. Senha de todos: `123456`.

| Perfil | E-mail |
|---|---|
| Admin | `admin@letsflow.com` |
| Signatário | `maria@letsflow.com` |
| Signatário | `joao@letsflow.com` |

> Se o login não funcionar, limpe os dados do app (Configurações → Apps → LetsFlow → Armazenamento → Limpar dados) e abra de novo.

### Roteiro rápido de teste
1. Entre como **Admin**, crie um documento com Maria e João, posicione um campo para cada um e envie.
2. Saia e entre como **Maria**: o documento aparece em Pendentes. Assine.
3. Saia e entre como **João** e assine também. O documento fica Concluído e o PDF final é gerado.
4. Entre de novo como **Admin** e abra os detalhes do documento para ver o histórico.

---

## Próximos passos (Sprint 2)

- Backend com Spring Boot, Spring Security e MongoDB.
- Upload do PDF por endpoint e sincronização entre aparelhos.
- Convite por e-mail e notificação push com deeplink para o documento.
- Primeiro acesso do signatário com criação da própria senha.

---

## Equipe — 2TDSOS

| Nome | RM |
|---|---|
| Angelo Romano | RM567274 |
| Amanda Uechis | RM566892|
| Aniole Lopes | RM567472 |
| Grace Camara | RM567589 |
| Mariana Malvão | RM567541 |
