package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

/*
 * Dados que a API aceita para criar ou editar um bolsista.
 * Senha opcional na edicao: vazia significa "mantem a que ja esta la".
 */
@Schema(description = "Dados para cadastro ou atualização de bolsista.")
public record BolsistaRequest(
        @NotBlank(message = "Nome e obrigatorio.")
        @Schema(description = "Nome completo", example = "Ana Pereira", requiredMode = Schema.RequiredMode.REQUIRED)
        String nome,

        @NotBlank(message = "E-mail e obrigatorio.")
        @Email(message = "Informe um e-mail valido.")
        @Schema(description = "E-mail de acesso institucional", example = "ana.pereira@aluno.sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Senha de acesso (obrigatória na criação, opcional na edição)", example = "12345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String senha,

        @Schema(description = "Data de nascimento", example = "2002-05-15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate dataNascimento,

        @Schema(description = "Curso de graduação do bolsista", example = "Sistemas para Internet", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String curso,

        @Schema(description = "Matrícula acadêmica única", example = "20240101", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String matricula,

        @Schema(description = "CPF do bolsista", example = "123.456.789-00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String cpf,

        @Schema(description = "Telefone de contato", example = "(48) 99999-1234", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String telefone,

        @Schema(description = "ID público do laboratório de lotação", example = "lab_5h8k0l4n7o9u1x3y6z8a", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String laboratorioId,

        @Schema(description = "Tipo de perfil de acesso", example = "BOLSISTA", allowableValues = {"BOLSISTA"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String tipoUsuario,

        @Schema(description = "URL pública da foto de perfil", example = "https://images.unsplash.com/photo-1534528741775-53994a69daeb", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String fotoUrl,

        @Schema(description = "Cargo no laboratório", example = "DESENVOLVEDOR", allowableValues = {"DESENVOLVEDOR", "PESQUISADOR", "LIDER_TECNICO", "DESIGNER", "AUXILIAR"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String cargo,

        @Schema(description = "Modalidade da bolsa", example = "PIBIC", allowableValues = {"PIBIC", "PIBITI", "EXTENSAO", "MONITORIA", "INSTITUCIONAL", "OUTRO"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String modalidadeBolsa,

        @Schema(description = "Valor mensal da bolsa em Reais (R$)", example = "700.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Double valorBolsa,

        @Schema(description = "Data de início da vigência", example = "2026-03-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate dataInicioBolsa,

        @Schema(description = "Data de término da vigência", example = "2026-12-31", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate dataFimBolsa,

        @Schema(description = "Biografia / Apresentação acadêmica", example = "Pesquisador com foco em sistemas distribuídos.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String bio) {
}
