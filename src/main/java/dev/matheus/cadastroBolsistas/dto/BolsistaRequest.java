package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

/*
 * o que a api aceita para criar ou editar um usuario.
 * senha opcional na edicao: vazia significa "mantem a que ja esta la", entao
 * o tamanho minimo dela e checado na mao no controller, nao aqui.
 */
@Schema(description = "Dados para cadastro ou atualização de bolsista / professor.")
public record BolsistaRequest(
        @NotBlank(message = "Nome e obrigatorio.")
        @Schema(description = "Nome completo", example = "Lucas Oliveira", requiredMode = Schema.RequiredMode.REQUIRED)
        String nome,

        @NotBlank(message = "E-mail e obrigatorio.")
        @Email(message = "Informe um e-mail valido.")
        @Schema(description = "E-mail de acesso institucional", example = "ana.pereira@aluno.sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Senha de acesso (obrigatória na criação, opcional na edição)", example = "12345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String senha,

        @Schema(description = "Data de nascimento", example = "2002-05-15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate dataNascimento,

        @Schema(description = "Curso de graduação do bolsista", example = "Engenharia de Software", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String curso,

        @Schema(description = "Matrícula acadêmica única", example = "20240101", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String matricula,

        @Schema(description = "CPF do bolsista", example = "123.456.789-00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String cpf,

        @Schema(description = "Telefone de contato", example = "(48) 99999-1234", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String telefone,

        @Schema(description = "ID do laboratório de lotação", example = "c1111111-1111-1111-1111-111111111111", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID laboratorioId,

        @Schema(description = "Tipo de perfil de acesso", example = "BOLSISTA", allowableValues = {"ADMIN", "PROFESSOR", "BOLSISTA"}, requiredMode = Schema.RequiredMode.REQUIRED)
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
