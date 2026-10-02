package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta da solicitação de recuperação de senha.")
public record EsqueciSenhaResponse(
        @Schema(description = "Mensagem informativa", example = "Código de verificação enviado para o e-mail informado (Válido por 15 minutos).")
        String mensagem,
        @Schema(description = "Código de 6 dígitos gerado (disponibilizado em ambiente de desenvolvimento)", example = "749201")
        String codigoDev
) {
}
