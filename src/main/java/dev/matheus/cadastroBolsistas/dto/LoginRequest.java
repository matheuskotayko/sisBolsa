package dev.matheus.cadastroBolsistas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credenciais necessárias para autenticação no sistema.")
public record LoginRequest(
        @Schema(description = "E-mail cadastrado do usuário", example = "admin@sisbolsa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Senha de acesso", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
        String senha) {
}
