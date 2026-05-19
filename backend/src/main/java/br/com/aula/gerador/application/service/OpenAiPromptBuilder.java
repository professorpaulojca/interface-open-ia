package br.com.aula.gerador.application.service;

import br.com.aula.gerador.domain.model.DocumentType;
import br.com.aula.gerador.domain.model.ExtractedText;

public final class OpenAiPromptBuilder {

    private static final String SYSTEM_INSTRUCTIONS_CODE_INTERPRETER = """
            Você é uma IA geradora de documentos Office para uma aplicação didática em Java Spring Boot.

            Regra obrigatória:
            - Use obrigatoriamente a ferramenta python/code interpreter.
            - Gere obrigatoriamente um arquivo %s válido.
            - Salve o arquivo com o nome: %s
            - Ao final, cite/anexe explicitamente o arquivo gerado na resposta final para que a API retorne uma anotação container_file_citation.

            Regras de conteúdo:
            - Se for planilha, crie abas, cabeçalhos e linhas coerentes com o pedido.
            - Se for Word, crie título, seções, parágrafos e, quando útil, tabelas internas.
            - Não devolva apenas texto. O objetivo é gerar o arquivo pronto para download.
            - Use português do Brasil.
            - Use bibliotecas Python adequadas quando disponíveis, como openpyxl para XLSX e python-docx para DOCX.
            - Se alguma biblioteca não estiver disponível, gere um arquivo Office Open XML válido de outra forma.
            - O arquivo final precisa ter extensão %s.
            """;

    private static final String SYSTEM_INSTRUCTIONS_SIMPLE = """
            Você é uma IA geradora de arquivos simples para uma aplicação didática em Java Spring Boot.
            Responda somente com o conteúdo final do arquivo.
            Não use Markdown, não use blocos de código e não explique o processo.
            Use português do Brasil.
            """;

    private OpenAiPromptBuilder() {
    }

    public static String buildCodeInterpreterInstructions(DocumentType documentType) {
        String documentTypeLabel = documentType == DocumentType.SPREADSHEET ? "XLSX" : "DOCX";
        String extension = documentType.extension();
        String filename = "resposta_ia" + extension;
        return SYSTEM_INSTRUCTIONS_CODE_INTERPRETER.formatted(documentTypeLabel, filename, extension);
    }

    public static String buildSimpleInstructions() {
        return SYSTEM_INSTRUCTIONS_SIMPLE;
    }

    public static String buildUserPayload(String currentPrompt, ExtractedText extracted) {
        String safePrompt = currentPrompt == null ? "" : currentPrompt;
        StringBuilder payload = new StringBuilder();
        payload.append("PROMPT DO USUÁRIO:\n").append(safePrompt).append("\n\n");

        if (extracted != null && !extracted.text().isBlank()) {
            payload.append("CONTEÚDO DO ARQUIVO ANEXADO PELO USUÁRIO (")
                    .append(extracted.filename()).append(")");
            if (extracted.truncated()) {
                payload.append(" [TRUNCADO em ").append(extracted.text().length())
                        .append(" de ").append(extracted.originalLength()).append(" caracteres]");
            }
            payload.append(":\n\"\"\"\n")
                    .append(extracted.text())
                    .append("\n\"\"\"\n\n");
        }

        payload.append("Tarefa: gere o documento final exatamente conforme solicitado pelo usuário acima. ")
                .append("Não use nenhum histórico ou contexto externo.");
        if (extracted != null) {
            payload.append(" Use o conteúdo do arquivo anexado acima como base/contexto.");
        }

        return payload.toString();
    }
}
