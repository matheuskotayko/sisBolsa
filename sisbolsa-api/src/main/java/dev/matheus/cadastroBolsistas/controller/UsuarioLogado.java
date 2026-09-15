package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/*
 * o JwtCookieFilter poe o Usuario como principal no SecurityContext a partir
 * do token, entao aqui e so leitura. o 401 e rede de seguranca: o spring
 * security ja barra /api/** sem token valido antes de chegar no controller.
 */
@Component
public class UsuarioLogado {

    public Usuario obrigatorio() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nao autenticado.");
        }
        return u;
    }

    public void exigirAdmin(Usuario usuario) {
        if (!usuario.isAdmin()) {
            throw new PermissaoNegadaException("Requer perfil de administrador.");
        }
    }

    public void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new PermissaoNegadaException(mensagem);
        }
    }
}
