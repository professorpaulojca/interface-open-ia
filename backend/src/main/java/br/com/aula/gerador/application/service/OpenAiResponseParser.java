package br.com.aula.gerador.application.service;

import br.com.aula.gerador.domain.exception.DocumentGenerationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static br.com.aula.gerador.application.service.OpenAiResponseField.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OpenAiResponseParser {

    private final ObjectMapper objectMapper;

    public OpenAiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parseJson(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (IOException e) {
            throw new DocumentGenerationException("Falha ao processar a resposta da OpenAI.", e);
        }
    }

    public String extractOutputText(JsonNode responseJson) {
        String direct = responseJson.path(OUTPUT_TEXT.fieldName()).asText("");
        if (!direct.isBlank()) {
            return direct;
        }

        StringBuilder builder = new StringBuilder();
        collectText(responseJson, builder);
        String text = builder.toString().trim();
        if (text.isBlank()) {
            throw new DocumentGenerationException("A OpenAI respondeu, mas não retornou texto para montar o arquivo.");
        }
        return text;
    }

    public Optional<ContainerFileCitation> chooseBestCitation(List<ContainerFileCitation> citations, String expectedExtension) {
        return citations.stream()
                .filter(c -> c.filename() != null && c.filename().toLowerCase().endsWith(expectedExtension))
                .findFirst()
                .or(() -> citations.stream().findFirst());
    }

    public List<ContainerFileCitation> collectCitations(JsonNode responseJson) {
        List<ContainerFileCitation> citations = new ArrayList<>();
        collectContainerFileCitations(responseJson, citations);
        return citations;
    }

    private void collectContainerFileCitations(JsonNode node, List<ContainerFileCitation> citations) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            if (isContainerFileCitation(node)) {
                citations.add(new ContainerFileCitation(
                        node.path(CONTAINER_ID.fieldName()).asText(),
                        node.path(FILE_ID.fieldName()).asText(),
                        node.path(FILENAME.fieldName()).asText("")
                ));
            }

            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                collectContainerFileCitations(fields.next().getValue(), citations);
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectContainerFileCitations(child, citations);
            }
        }
    }

    private boolean isContainerFileCitation(JsonNode node) {
        return "container_file_citation".equals(node.path(TYPE.fieldName()).asText())
                && !node.path(CONTAINER_ID.fieldName()).asText("").isBlank()
                && !node.path(FILE_ID.fieldName()).asText("").isBlank();
    }

    private void collectText(JsonNode node, StringBuilder builder) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            if ("output_text".equals(node.path(TYPE.fieldName()).asText())
                    && !node.path(TEXT.fieldName()).asText("").isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(System.lineSeparator()).append(System.lineSeparator());
                }
                builder.append(node.path(TEXT.fieldName()).asText());
            }

            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                collectText(fields.next().getValue(), builder);
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectText(child, builder);
            }
        }
    }

    public record ContainerFileCitation(String containerId, String fileId, String filename) {
    }
}
