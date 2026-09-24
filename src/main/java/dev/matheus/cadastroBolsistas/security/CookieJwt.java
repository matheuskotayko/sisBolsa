package dev.matheus.cadastroBolsistas.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

/*
 * o token viaja em cookie httpOnly, nao em localStorage, por dois motivos:
 * navegacao direta do browser nao manda header Authorization, e os downloads
 * de csv sao link comum, que tambem nao manda header.
 *
 * httpOnly tira o token do alcance de javascript (xss) e SameSite=Strict
 * cobre o csrf que o cookie automatico traria de volta.
 */
public final class CookieJwt {

    public static final String NOME = "token";

    private CookieJwt() {}

    public static ResponseCookie gravarCookie(String token, long expiracaoMinutos) {
        return ResponseCookie.from(NOME, token)
                .httpOnly(true)
                .path("/")
                .maxAge(expiracaoMinutos * 60)
                .sameSite("Strict")
                .build();
    }

    public static ResponseCookie limparCookie() {
        return ResponseCookie.from(NOME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    public static String ler(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var c : request.getCookies()) {
            if (NOME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
