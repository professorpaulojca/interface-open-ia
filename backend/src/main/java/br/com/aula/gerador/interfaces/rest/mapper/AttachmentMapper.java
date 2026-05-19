package br.com.aula.gerador.interfaces.rest.mapper;

import br.com.aula.gerador.domain.model.Attachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public final class AttachmentMapper {

    private AttachmentMapper() {
    }

    public static Attachment toDomain(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return Attachment.create(
                    file.getOriginalFilename(),
                    file.getBytes(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o conteúdo do arquivo anexado.", e);
        }
    }
}
