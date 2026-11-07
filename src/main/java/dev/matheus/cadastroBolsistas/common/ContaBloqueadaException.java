package dev.matheus.cadastroBolsistas.common;

public class ContaBloqueadaException extends RuntimeException {
    public ContaBloqueadaException(String mensagem) {
        super(mensagem);
    }
}
