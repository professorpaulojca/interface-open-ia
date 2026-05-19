package br.com.aula.gerador.domain.exception;

public class ContextLengthExceededException extends DomainException {

    public ContextLengthExceededException(String message) {
        super(message);
    }
}
