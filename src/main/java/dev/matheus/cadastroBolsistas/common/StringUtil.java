package dev.matheus.cadastroBolsistas.common;

/*
 * utilitario para manipulacao de strings no sistema.
 * centraliza as operacoes de limpeza e validacao de campos de formulario.
 */
public class StringUtil {

    public static String limpar(String valor) {
        return valor != null ? valor.trim() : "";
    }

    public static boolean estaVazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
