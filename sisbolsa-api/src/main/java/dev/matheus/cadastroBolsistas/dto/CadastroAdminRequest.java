package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * confirmaSenha == senha continua sendo checado na mao no controller:
 * e uma regra entre dois campos, bean validation de campo unico nao cobre.
 */
@Schema(description = "Dados para cadastro inicial de administrador (limitado a 3 no sistema).")
public record CadastroAdminRequest(
        @NotBlank(message = "O nome deve ter pelo menos 3 caracteres.")
        @Size(min = 3, message = "O nome deve ter pelo menos 3 caracteres.")
        @Schema(description = "Nome completo do administrador", example = "Administrador do Sistema", requiredMode = Schema.RequiredMode.REQUIRED)
        String nome,

        @NotBlank(message = "Informe um e-mail valido.")
        @Email(message = "Informe um e-mail valido.")
        @Schema(description = "E-mail de acesso institucional", example = "admin@sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "A senha deve ter pelo menos 6 caracteres.")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.")
        @Schema(description = "Senha de acesso (mínimo 6 caracteres)", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
        String senha,

        @NotBlank(message = "As senhas nao coincidem.")
        @Schema(description = "Confirmação da senha", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
        String confirmaSenha) {
}
