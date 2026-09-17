package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erro de validação de um campo específico do corpo da requisição.")
public record ErroValidacaoCampo(
        @Schema(description = "Nome do campo que falhou na validação", example = "email")
        String campo,

        @Schema(description = "Mensagem explicativa do erro de validação", example = "Informe um e-mail valido.")
        String mensagem) {
}
