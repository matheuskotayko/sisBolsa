package dev.matheus.cadastroBolsistas.dto.response;

/** arquivo pronto para download: o controller so escolhe o content-type. */
public record ComprovanteResponse(String nomeArquivo, byte[] conteudo) {
}
