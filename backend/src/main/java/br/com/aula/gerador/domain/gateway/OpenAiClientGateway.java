package br.com.aula.gerador.domain.gateway;

import br.com.aula.gerador.domain.model.DocumentType;
import br.com.aula.gerador.domain.model.ExtractedText;
import br.com.aula.gerador.domain.model.GeneratedDocument;

public interface OpenAiClientGateway {

    GeneratedDocument generateSimpleDocument(String currentPrompt, ExtractedText extractedText);

    GeneratedDocument generateWithCodeInterpreter(String currentPrompt, DocumentType documentType, ExtractedText extractedText);
}
