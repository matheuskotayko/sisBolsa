package dev.matheus.cadastroBolsistas.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RecursoNaoEncontradoException extends ResponseStatusException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, mensagem);
    }
}
