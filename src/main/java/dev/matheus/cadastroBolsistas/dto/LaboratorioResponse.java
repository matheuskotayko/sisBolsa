package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Laboratorio;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados detalhados do laboratório com métricas de ocupação.")
public record LaboratorioResponse(
        @Schema(description = "Identificador público único do laboratório", example = "lab_5h8k0l4n7o9u1x3y6z8a")
        String id,

        @Schema(description = "Nome do laboratório", example = "Laboratório de Sistemas Inteligentes (LSI)")
        String nome,

        @Schema(description = "Área de pesquisa", example = "Inteligência Artificial")
        String areaPesquisa,

        @Schema(description = "Status operacional", example = "Ativo")
        String status,

        @Schema(description = "Capacidade total de vagas", example = "10")
        int capacidade,

        @Schema(description = "ID público do professor coordenador", example = "prf_7k0m2n6p9q1w3z5a8b0c")
        String coordenadorId,

        @Schema(description = "Nome do professor coordenador", example = "Prof. Dra. Ana Mendes")
        String coordenador,

        @Schema(description = "Indica se o laboratório está ativo", example = "true")
        boolean ativo,

        @Schema(description = "Quantidade atual de bolsistas alocados", example = "6")
        int totalBolsistas,

        @Schema(description = "Percentual de ocupação em relação à capacidade (0 a 100%)", example = "60.0")
        double percentualOcupacao) {

    public static LaboratorioResponse de(Laboratorio l) {
        return de(l, 0);
    }

    public static LaboratorioResponse de(Laboratorio l, int totalBolsistas) {
        if (l == null) {
            return null;
        }
        double percentual = l.getCapacidade() > 0
                ? (totalBolsistas / (double) l.getCapacidade()) * 100.0
                : 0.0;
        return new LaboratorioResponse(
                l.getPublicId(), l.getNome(), l.getAreaPesquisa(), l.getStatus(), l.getCapacidade(),
                l.getCoordenadorPublicId(),
                l.getCoordenador(), l.isAtivo(), totalBolsistas, percentual);
    }
}
