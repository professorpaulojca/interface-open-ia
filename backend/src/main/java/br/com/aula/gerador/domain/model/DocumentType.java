package br.com.aula.gerador.domain.model;

import java.util.Locale;
import java.util.Set;

public enum DocumentType {
    SPREADSHEET(".xlsx", Set.of("excel", "xlsx", "planilha", "tabular", "csv")),
    WORD(".docx", Set.of("word", "docx", ".doc", "documento", "redação", "redacao",
            "relatório", "relatorio", "carta", "ofício", "oficio", "contrato",
            "artigo", "monografia", "texto formatado")),
    TEXT(".txt", Set.of());

    private final String extension;
    private final Set<String> keywords;

    DocumentType(String extension, Set<String> keywords) {
        this.extension = extension;
        this.keywords = keywords;
    }

    public String extension() {
        return extension;
    }

    public static DocumentType detectFromPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return TEXT;
        }
        String lower = prompt.toLowerCase(Locale.ROOT);
        if (matchesAny(lower, SPREADSHEET.keywords)) {
            return SPREADSHEET;
        }
        if (matchesAny(lower, WORD.keywords)) {
            return WORD;
        }
        return TEXT;
    }

    public boolean usesCodeInterpreter() {
        return this == SPREADSHEET || this == WORD;
    }

    private static boolean matchesAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
