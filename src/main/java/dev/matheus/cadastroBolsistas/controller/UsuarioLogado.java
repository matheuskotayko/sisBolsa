package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UsuarioLogado {

    public Usuario obrigatorio() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nao autenticado.");
        }
        return u;
    }
}
