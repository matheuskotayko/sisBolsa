package dev.matheus.cadastroBolsistas.dto;

import dev.matheus.cadastroBolsistas.model.Auditoria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Registro de evento na trilha de auditoria do sistema.")
public record AuditoriaResponse(
        @Schema(description = "Identificador único do log (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "ID do usuário que executou a ação", example = "4fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID usuarioId,

        @Schema(description = "Nome do usuário responsável pela ação", example = "Administrador do Sistema")
        String usuarioNome,

        @Schema(description = "Tipo de ação executada", example = "LOGIN", allowableValues = {"LOGIN", "LOGIN_FALHA", "LOGIN_BLOQUEADO", "LOGOUT", "CRIAR_BOLSISTA", "ATUALIZAR_BOLSISTA", "EXCLUIR_BOLSISTA", "CRIAR_LABORATORIO", "ATUALIZAR_LABORATORIO", "EXCLUIR_LABORATORIO", "CRIAR_PROJETO", "ATUALIZAR_PROJETO", "EXCLUIR_PROJETO", "VINCULAR_MEMBRO", "DESVINCULAR_MEMBRO", "CRIAR_FREQUENCIA", "ATUALIZAR_FREQUENCIA", "EXCLUIR_FREQUENCIA", "EMISSAO_COMPROVANTE_PDF", "ALTERAR_SENHA", "ATUALIZAR_PERFIL", "SOLICITAR_RECUPERACAO_SENHA", "REDEFINICAO_SENHA"})
        String acao,

        @Schema(description = "Entidade afetada", example = "AUTH", allowableValues = {"AUTH", "USUARIO", "BOLSISTA", "PROFESSOR", "LABORATORIO", "PROJETO", "FREQUENCIA"})
        String entidade,

        @Schema(description = "Descrição detalhada do evento", example = "Login efetuado com sucesso (ADMIN)")
        String detalhes,

        @Schema(description = "Endereço IP de origem da requisição", example = "192.168.1.100")
        String ipOrigem,

        @Schema(description = "Data e hora do registro", example = "2026-08-29T14:30:00")
        LocalDateTime dataHora) {

    public static AuditoriaResponse de(Auditoria a) {
        if (a == null) return null;
        return new AuditoriaResponse(
                a.getId(),
                a.getUsuarioId(),
                a.getUsuarioNome(),
                a.getAcao(),
                a.getEntidade(),
                a.getDetalhes(),
                a.getIpOrigem(),
                a.getDataHora());
    }
}
