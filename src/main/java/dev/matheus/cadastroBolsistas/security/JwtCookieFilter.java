package dev.matheus.cadastroBolsistas.security;

import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.JwtService;
import dev.matheus.cadastroBolsistas.service.LoginService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/*
 * le o jwt do header Authorization e autentica a requisicao.
 *
 * sem sessao de servidor: o token e a unica fonte de verdade. a cada
 * requisicao o usuario e recarregado do banco a partir do e-mail do
 * claim e vira o principal do SecurityContext.
 */
@Component
public class JwtCookieFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final LoginService loginService;

    public JwtCookieFilter(JwtService jwtService, LoginService loginService) {
        this.jwtService = jwtService;
        this.loginService = loginService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extrairBearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtService.validar(token);
            if (claims != null) {
                autenticar(claims);
            }
        }
        chain.doFilter(request, response);
    }

    private String extrairBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void autenticar(Claims claims) {
        String email = claims.getSubject();
        String tipo = claims.get("tipo", String.class);

        Usuario usuario = loginService.buscarPorEmail(email);
        if (usuario == null) {
            return;
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + tipo));
        var auth = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
