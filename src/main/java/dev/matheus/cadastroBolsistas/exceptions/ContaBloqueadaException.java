package dev.matheus.cadastroBolsistas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ContaBloqueadaException extends ResponseStatusException {
    public ContaBloqueadaException(String mensagem) {
        super(HttpStatus.TOO_MANY_REQUESTS, mensagem);
    }
}
