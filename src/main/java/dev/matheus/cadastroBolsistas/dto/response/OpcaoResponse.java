package dev.matheus.cadastroBolsistas.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Opção de um domínio fixo (enum) para preencher listas de seleção.")
public record OpcaoResponse(
        @Schema(description = "Valor enviado nas requisições", example = "PESQUISADOR")
        String valor,

        @Schema(description = "Texto para exibição", example = "Pesquisador")
        String descricao) {
}
