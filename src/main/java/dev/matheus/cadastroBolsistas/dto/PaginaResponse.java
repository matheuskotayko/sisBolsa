package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Envelope genérico para respostas paginadas de listas.")
public record PaginaResponse<T>(
        @Schema(description = "Lista de itens da página atual")
        List<T> itens,

        @Schema(description = "Número da página atual (1-indexado)", example = "1")
        int pagina,

        @Schema(description = "Total de páginas disponíveis", example = "5")
        int totalPaginas,

        @Schema(description = "Total geral de registros encontrados", example = "42")
        int totalItens) {
}
