package dev.matheus.cadastroBolsistas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
 * duas cadeias: /api/** exige token JWT e responde 401/403 em json; o
 * resto (swagger-ui, openapi docs) fica aberto sem autenticacao.
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
                    "/api/v1/auth/login",
                    "/api/v1/auth/cadastro-admin",
                    "/api/v1/auth/password-reset-requests",
                    "/api/v1/auth/password-resets"
                ).permitAll()
                /*
                 * gates puros de papel ficam aqui, no proprio spring security. o que
                 * depende do dado (professor so mexe no lab que coordena, bolsista so
                 * no proprio cadastro) continua no service, porque um matcher de url
                 * nao sabe de quem e a linha que esta sendo editada.
                 */
                .requestMatchers("/api/v1/relatorios/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/professores/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/cursos").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/laboratorios").hasRole("ADMIN")
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
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
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
