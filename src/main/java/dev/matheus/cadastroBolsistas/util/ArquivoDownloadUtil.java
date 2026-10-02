package dev.matheus.cadastroBolsistas.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/*
 * monta o ResponseEntity<byte[]> dos downloads (csv/pdf) com content-type e
 * content-disposition corretos, em vez de escrever direto no
 * HttpServletResponse - assim o swagger consegue documentar o tipo binario
 * de verdade.
 */
public class ArquivoDownloadUtil {

    private ArquivoDownloadUtil() {
    }

    public static ResponseEntity<byte[]> csv(String nomeArquivo, String conteudo) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nomeArquivo)
                .body(conteudo.getBytes(StandardCharsets.UTF_8));
    }

    public static ResponseEntity<byte[]> pdf(String nomeArquivo, byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + nomeArquivo)
                .body(bytes);
    }
}
