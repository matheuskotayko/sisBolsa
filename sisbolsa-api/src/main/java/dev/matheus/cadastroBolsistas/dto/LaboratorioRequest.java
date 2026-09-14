package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Dados para criação ou atualização de um laboratório de pesquisa.")
public record LaboratorioRequest(
        @NotBlank(message = "Nome do laboratorio e obrigatorio.")
        @Schema(description = "Nome do laboratório", example = "Laboratório de Sistemas Inteligentes (LSI)", requiredMode = Schema.RequiredMode.REQUIRED)
        String nome,

        @Schema(description = "Área de pesquisa principal", example = "Inteligência Artificial e Robótica", requiredMode = Schema.RequiredMode.REQUIRED)
        String areaPesquisa,

        /* em branco vira "Ativo" no service, entao nao pode ser @NotBlank aqui */
        @Schema(description = "Status operacional", example = "Ativo", allowableValues = {"Ativo", "Em Manutenção", "Inativo"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String status,

        @NotNull(message = "Capacidade precisa ser maior que zero.")
        @Min(value = 1, message = "Capacidade precisa ser maior que zero.")
        @Schema(description = "Capacidade máxima de bolsistas e pesquisadores simultâneos", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer capacidade,

        @Schema(description = "ID do professor coordenador responsável", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID coordenadorId) {
}
