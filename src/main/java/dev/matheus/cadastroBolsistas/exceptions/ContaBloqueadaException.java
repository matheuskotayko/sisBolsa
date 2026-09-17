package dev.matheus.cadastroBolsistas.exceptions;

public class ContaBloqueadaException extends RuntimeException {
    public ContaBloqueadaException(String mensagem) {
        super(mensagem);
    }
}
