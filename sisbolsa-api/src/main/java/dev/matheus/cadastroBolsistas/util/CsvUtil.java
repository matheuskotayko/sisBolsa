package dev.matheus.cadastroBolsistas.util;

/*
 * escapa valor pra celula csv (aspas duplicadas + campo entre aspas).
 * antes vivia repetido em 3 controllers de exportacao.
 */
public class CsvUtil {

    private CsvUtil() {
    }

    public static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
