package br.com.aula.gerador.domain.gateway;

import br.com.aula.gerador.domain.model.Attachment;
import br.com.aula.gerador.domain.model.ExtractedText;

public interface AttachmentTextExtractorGateway {

    ExtractedText extract(Attachment attachment);
}
