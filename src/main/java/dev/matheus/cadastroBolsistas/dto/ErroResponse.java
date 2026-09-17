package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estrutura padrão para retorno de erros e exceções da API.")
public record ErroResponse(
        @Schema(description = "Mensagem explicativa do erro", example = "E-mail ou senha incorretos.")
        String mensagem) {
}
