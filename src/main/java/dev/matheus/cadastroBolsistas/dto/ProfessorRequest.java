package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
 * senha opcional na edicao: vazia significa "mantem a que ja esta la", entao
 * o tamanho minimo dela e checado na mao no controller, nao aqui.
 */
@Schema(description = "Dados para cadastro ou atualização de professor coordenador.")
public record ProfessorRequest(
        @NotBlank(message = "Nome e obrigatorio.")
        @Schema(description = "Nome completo", example = "Dr. Roberto Mendes", requiredMode = Schema.RequiredMode.REQUIRED)
        String nome,

        @NotBlank(message = "E-mail e obrigatorio.")
        @Email(message = "Informe um e-mail valido.")
        @Schema(description = "E-mail de acesso institucional", example = "roberto.mendes@sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Senha de acesso (obrigatória na criação, opcional na edição)", example = "12345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String senha,

        @Schema(description = "URL pública da foto de perfil", example = "https://i.pravatar.cc/150?img=11", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String fotoUrl,

        @Schema(description = "Biografia / Apresentação acadêmica", example = "Coordenador do laboratório de desenvolvimento de software.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String bio) {
}
