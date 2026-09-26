package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padronizada de mensagens informativas da API.")
public record MensagemResponse(
        @Schema(description = "Mensagem informativa", example = "Operação realizada com sucesso.")
        String mensagem
) {
}
