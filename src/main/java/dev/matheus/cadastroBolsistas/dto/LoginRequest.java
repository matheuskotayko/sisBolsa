package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais necessárias para autenticação no sistema.")
public record LoginRequest(
        @Schema(description = "E-mail cadastrado do usuário", example = "admin@sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail com formato inválido.")
        String email,

        @Schema(description = "Senha de acesso", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Senha é obrigatória.")
        String senha) {

    public LoginRequest {
        email = email != null ? email.trim() : null;
        senha = senha != null ? senha.trim() : null;
    }
}
