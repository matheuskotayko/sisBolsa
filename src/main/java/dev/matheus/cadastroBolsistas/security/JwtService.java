package dev.matheus.cadastroBolsistas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/*
 * emite e valida os tokens jwt. o assunto do token e o email do usuario;
 * o tipo (ADMIN, BOLSISTA, PROFESSOR) vai como claim para o filtro montar
 * a authority sem precisar bater no banco antes da hora.
 */
@Service
public class JwtService {

    private static final String CLAIM_TIPO = "tipo";

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(String email, String tipoUsuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_TIPO, tipoUsuario)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoMinutos * 60)))
                .signWith(chave)
                .compact();
    }

    /*
     * devolve null quando o token e invalido, expirado ou foi adulterado -
     * quem chama so precisa saber que nao da para confiar.
     */
    public Claims validar(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getExpiracaoMinutos() {
        return expiracaoMinutos;
    }
}
