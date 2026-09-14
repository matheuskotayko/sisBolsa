package dev.matheus.cadastroBolsistas.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
 * duas cadeias, porque as duas frentes falham de jeitos diferentes:
 * - /api/** responde 401/403 em json, que e o que um cliente rest espera
 * - o resto redireciona para a tela de login, que e o que um browser espera
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtCookieFilter jwtCookieFilter) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            /* stateless de verdade: nada de sessao de servidor, o token e a unica fonte de autenticacao */
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/cadastro-admin",
                    "/api/auth/admins-restantes",
                    "/api/auth/password-reset-requests",
                    "/api/auth/password-resets"
                ).permitAll()
                .requestMatchers("/api/relatorios/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> escreverErro(res, HttpStatus.UNAUTHORIZED, "Nao autenticado."))
                .accessDeniedHandler((req, res, e) -> escreverErro(res, HttpStatus.FORBIDDEN, "Acesso negado.")))
            .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http, JwtCookieFilter jwtCookieFilter) throws Exception {
        http
            /*
             * csrf desligado porque nenhum formulario jsp manda token. o que
             * segura o csrf aqui e o SameSite=Strict do cookie do jwt.
             * quando o front virar estatico (etapa 6) isso volta a ser avaliado.
             */
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                /*
                 * swagger aberto: e a documentacao do trabalho e precisa abrir
                 * sem login. nao expoe dado nenhum, so a forma dos endpoints -
                 * que continuam exigindo token.
                 */
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                /*
                 * as paginas estaticas e o bundle do react sao a casca da aplicacao.
                 * quem protege os dados e a api (/api/**).
                 */
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/favicon.ico", "/*.html", "/css/**", "/js/**").permitAll()
                .anyRequest().permitAll())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void escreverErro(jakarta.servlet.http.HttpServletResponse res, HttpStatus status, String mensagem)
            throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
