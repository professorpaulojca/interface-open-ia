package br.com.aula.gerador.application.service;

public enum OpenAiResponseField {
    TYPE("type"),
    CONTAINER_ID("container_id"),
    FILE_ID("file_id"),
    FILENAME("filename"),
    OUTPUT_TEXT("output_text"),
    TEXT("text"),
    ERROR("error"),
    CODE("code"),
    MESSAGE("message"),
    MODEL("model"),
    INSTRUCTIONS("instructions"),
    INPUT("input"),
    TOOL_CHOICE("tool_choice"),
    TOOLS("tools"),
    CONTAINER("container"),
    MEMORY_LIMIT("memory_limit"),
    ROLE("role"),
    CONTENT("content");

    private final String fieldName;

    OpenAiResponseField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }

    public String path() {
        return fieldName;
    }
}
