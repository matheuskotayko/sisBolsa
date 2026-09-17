package dev.matheus.cadastroBolsistas.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    public static void gravar(HttpServletResponse response, String token, long expiracaoMinutos) {
        Cookie cookie = new Cookie(NOME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (expiracaoMinutos * 60));
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static void limpar(HttpServletResponse response) {
        Cookie cookie = new Cookie(NOME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static String ler(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if (NOME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
