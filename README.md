# Gerador IA de documentos DOCX/XLSX com Code Interpreter

Projeto didático com:

- Frontend simples em HTML, CSS e JavaScript Vanilla.
- Backend Java Spring Boot.
- Histórico acumulado de prompts salvo em arquivo texto.
- Chamada para a OpenAI Responses API com a ferramenta `code_interpreter`.
- Download do arquivo gerado pela própria IA no container da OpenAI.

## Arquitetura

Veja o diagrama detalhado da arquitetura em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).

## Ideia da versão avançada

Nesta versão, o Java **não monta** o Excel nem o Word com Apache POI.

O fluxo é:

```text
Usuário digita o prompt
→ Backend grava o prompt em data/prompts.txt
→ Backend envia o histórico acumulado para a OpenAI
→ OpenAI usa Code Interpreter para gerar um .xlsx ou .docx
→ OpenAI retorna uma anotação container_file_citation
→ Backend extrai container_id e file_id
→ Backend baixa o arquivo binário
→ Navegador baixa o arquivo final
```

## Estrutura do projeto

```text
gerador-ia-code-interpreter/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/br/com/aula/gerador/
│       │   ├── GeradorIaApplication.java
│       │   ├── config/CorsConfig.java
│       │   ├── controller/DocumentController.java
│       │   ├── controller/ApiExceptionHandler.java
│       │   ├── dto/
│       │   └── service/
│       └── resources/application.properties
└── frontend/
    ├── index.html
    ├── style.css
    └── app.js
```

## Atenção sobre chave de API

Nunca coloque a chave da OpenAI diretamente no código.

Configure a chave por variável de ambiente.

No Windows PowerShell:

```powershell
setx OPENAI_API_KEY "sua_chave_nova_aqui"
setx OPENAI_MODEL "gpt-4.1"
```

Depois feche e abra o terminal novamente.

> Observação: use um modelo com suporte à ferramenta Code Interpreter. O projeto deixa `gpt-4.1` como padrão porque é o modelo usado nos exemplos atuais da documentação da OpenAI para Code Interpreter. Você pode trocar por outro modelo compatível usando `OPENAI_MODEL`.

## Rodando o backend

Entre na pasta do backend:

```bash
cd backend
```

Execute:

```bash
mvn spring-boot:run
```

O backend ficará disponível em:

```text
http://localhost:8080
```

## Rodando o frontend

Abra o arquivo:

```text
frontend/index.html
```

Também é possível usar a extensão Live Server do Visual Studio Code.

## Endpoints

### Gerar documento

```http
POST /api/documentos/gerar
Content-Type: multipart/form-data
```

Campos esperados no multipart:

- `prompt` (obrigatório)
- `file` (opcional, extensões permitidas: `pdf`, `docx`, `xlsx`, `txt`)

Exemplo com `curl`:

```bash
curl -X POST http://localhost:8080/api/documentos/gerar \
  -F "prompt=Resuma o conteúdo do anexo em uma planilha xlsx" \
  -F "file=@./meu_documento.pdf"
```

A resposta agora é JSON com metadados + URL de download:

```json
{
  "mensagem": "Arquivo gerado com sucesso.",
  "id": "...",
  "filename": "resposta_ia.xlsx",
  "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "size": 12345,
  "downloadUrl": "/api/documentos/download/..."
}
```

### Consultar histórico

```http
GET /api/documentos/historico
```

### Limpar histórico

```http
DELETE /api/documentos/historico
```

## Onde o histórico é salvo

Por padrão:

```text
backend/data/prompts.txt
```

A última resposta bruta da OpenAI é salva para depuração em:

```text
backend/data/last-openai-response.json
```

Esse arquivo ajuda a entender se a resposta trouxe `container_file_citation`, `container_id` e `file_id`.

## Como o backend baixa o arquivo

Depois que a OpenAI gera o arquivo, a resposta contém uma anotação parecida com:

```json
{
  "type": "container_file_citation",
  "container_id": "cntr_...",
  "file_id": "cfile_...",
  "filename": "resposta_ia.xlsx"
}
```

O backend então faz:

```http
GET /v1/containers/{container_id}/files/{file_id}/content
Authorization: Bearer OPENAI_API_KEY
```

E devolve os bytes para o navegador.

## Diferença para a versão didática com Apache POI

### Versão com Apache POI

```text
OpenAI devolve JSON estruturado
→ Java monta o arquivo com Apache POI
```

Melhor para ensinar Java, OO e bibliotecas de documentos.

### Versão com Code Interpreter

```text
OpenAI gera o arquivo pronto
→ Java só baixa e entrega
```

Mais impressionante e mais curta, mas mais dependente da plataforma da OpenAI.

## Sugestões de teste

Prompt para Excel:

```text
Gere uma planilha Excel com 10 produtos de papelaria, contendo produto, quantidade, preço unitário, subtotal e uma linha final de total geral.
```

Prompt para Word:

```text
Gere um documento Word explicando de forma didática o que é uma API REST, com título, seções, exemplos e conclusão.
```
