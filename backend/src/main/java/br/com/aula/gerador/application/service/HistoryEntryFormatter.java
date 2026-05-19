package br.com.aula.gerador.application.service;

import br.com.aula.gerador.domain.model.DownloadUrl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class HistoryEntryFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String PROMPT_ENTRY_TEMPLATE = """

            ============================================================
            Data/Hora: %s
            Prompt:
            %s
            """;

    private static final String FILE_ENTRY_TEMPLATE = """
            Arquivo gerado: %s
            Link de download: <a href="%s" target="_blank" rel="noopener noreferrer">%s</a>
            """;

    private HistoryEntryFormatter() {
    }

    public static String formatPromptEntry(LocalDateTime timestamp, String prompt) {
        return String.format(PROMPT_ENTRY_TEMPLATE,
                timestamp.format(TIMESTAMP_FORMATTER),
                prompt.trim());
    }

    public static String formatGeneratedFileEntry(String filename, DownloadUrl downloadUrl) {
        String escapedFilename = escapeHtml(filename);
        String escapedUrl = escapeHtml(downloadUrl.toAbsoluteUrl());
        return String.format(FILE_ENTRY_TEMPLATE, filename, escapedUrl, escapedFilename);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
