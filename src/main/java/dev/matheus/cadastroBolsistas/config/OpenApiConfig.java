package dev.matheus.cadastroBolsistas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/*
 * configuracao global da documentacao openapi 3 / swagger ui do sisbolsa.
 */
@Configuration
public class OpenApiConfig {

    private static final String COOKIE_JWT = "cookieJwt";

    @Bean
    public OpenAPI sisBolsaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SisBolsa API - Sistema de Gestão de Bolsistas e Laboratórios")
                        .version("1.0.0")
                        .description("""
                                ### Visão Geral
                                API RESTful para gerenciamento completo de bolsistas, professores, laboratórios de pesquisa, projetos acadêmicos, controle de frequência/horas e trilha de auditoria.

                                ### Autenticação e Segurança
                                - **Autenticação Baseada em JWT:** Para autenticar, utilize `POST /api/auth/login`. A API grava automaticamente um cookie `httpOnly` (`token`) com proteção `SameSite=Strict`.
                                - **Rate Limiting & Anti-Brute Force:** Limite de 5 tentativas consecutivas com erro. Em caso de excesso, a conta é bloqueada temporariamente por 5 minutos (HTTP 429).
                                - **Controle de Acesso Baseado em Perfis (RBAC):**
                                  - **`ADMIN`:** Acesso total irrestrito a todos os recursos, auditoria, configurações e relatórios globais.
                                  - **`PROFESSOR`:** Gerenciamento dos laboratórios que coordena, seus projetos associados e bolsistas vinculados.
                                  - **`BOLSISTA`:** Apontamento de frequência própria, visualização do seu laboratório, projetos e comprovantes em PDF.
                                """)
                        .contact(new Contact()
                                .name("Suporte SisBolsa")
                                .email("admin@sisbolsa.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .tags(List.of(
                        new Tag().name("Autenticação").description("Login, logout, verificação de sessão (/me), perfil e fluxo de recuperação de senha."),
                        new Tag().name("Bolsistas & Usuários").description("Cadastro, consulta paginada, edição de vigência/bolsas e soft-delete de bolsistas e professores."),
                        new Tag().name("Laboratórios").description("Gestão de laboratórios de pesquisa, vinculação de coordenadores e controle de ocupação."),
                        new Tag().name("Projetos").description("Gestão de projetos de pesquisa, vinculação de membros e anexação de entregáveis/repositórios."),
                        new Tag().name("Frequência & Horas").description("Apontamento de horas trabalhadas, resumo mensal, exportação em CSV e emissão de comprovantes em PDF."),
                        new Tag().name("Relatórios & Estatísticas").description("Métricas de ocupação, carga horária mensal, projetos ativos e exportação CSV gerencial."),
                        new Tag().name("Auditoria").description("Rastreamento de acessos e trilha de auditoria de ações críticas no sistema.")
                ))
                .components(new Components().addSecuritySchemes(COOKIE_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("token")
                                .description("Token JWT gravado automaticamente em cookie HttpOnly após autenticação em /api/auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_JWT));
    }
}
