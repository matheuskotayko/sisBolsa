package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Laboratorio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Dados detalhados do laboratório com métricas de ocupação.")
public record LaboratorioResponse(
        @Schema(description = "Identificador único do laboratório (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Nome do laboratório", example = "Laboratório de Sistemas Inteligentes (LSI)")
        String nome,

        @Schema(description = "Área de pesquisa", example = "Inteligência Artificial")
        String areaPesquisa,

        @Schema(description = "Status operacional", example = "Ativo")
        String status,

        @Schema(description = "Capacidade total de vagas", example = "10")
        int capacidade,

        @Schema(description = "ID do professor coordenador", example = "4fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID coordenadorId,

        @Schema(description = "Nome do professor coordenador", example = "Prof. Dra. Ana Mendes")
        String coordenador,

        @Schema(description = "Indica se o laboratório está ativo", example = "true")
        boolean ativo,

        @Schema(description = "Quantidade atual de bolsistas alocados", example = "6")
        int totalBolsistas,

        @Schema(description = "Percentual de ocupação em relação à capacidade (0 a 100%)", example = "60.0")
        double percentualOcupacao) {

    public static LaboratorioResponse de(Laboratorio l, int totalBolsistas) {
        if (l == null) {
            return null;
        }
        double percentual = l.getCapacidade() > 0
                ? (totalBolsistas / (double) l.getCapacidade()) * 100.0
                : 0.0;
        return new LaboratorioResponse(
                l.getId(), l.getNome(), l.getAreaPesquisa(), l.getStatus(), l.getCapacidade(),
                l.getCoordenadorId(),
                l.getCoordenador(), l.isAtivo(), totalBolsistas, percentual);
    }
}
