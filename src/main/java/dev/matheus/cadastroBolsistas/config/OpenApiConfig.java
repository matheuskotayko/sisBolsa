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

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI sisBolsaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SisBolsa API - Sistema de Gestão de Bolsistas e Laboratórios")
                        .version("1.0.0")
                        .description("""
                                ### Visão Geral
                                API RESTful para gerenciamento completo de bolsistas, professores coordenadores, administradores, laboratórios de pesquisa, projetos acadêmicos e controle de frequência/horas.

                                ### Identificadores Públicos (Public IDs)
                                A API utiliza identificadores públicos opacos e seguros no formato `prefixo_hashBase62` (ex: `adm_...`, `prf_...`, `bol_...`, `lab_...`, `prj_...`, `cur_...`, `frq_...`, `usr_...`) para desacoplar as chaves primárias internas e prevenir enumeração de recursos e exposição de IDs de banco de dados. Todas as rotas de busca, atualização, exclusão e vínculos utilizam exclusivamente estes IDs públicos.

                                ### Autenticação e Segurança
                                - **Autenticação Bearer JWT:** Para autenticar, utilize `POST /api/v1/auth/login`. A resposta contém o token no header `X-Auth-Token`. Use em requisições subsequentes: `Authorization: Bearer <token>`.
                                - **Rate Limiting & Anti-Brute Force:** Limite de 5 tentativas consecutivas com erro. Em caso de excesso, a conta é bloqueada temporariamente por 5 minutos (HTTP 429).
                                - **Controle de Acesso Baseado em Perfis (RBAC):**
                                  - **`ADMIN`:** Acesso total irrestrito a todos os recursos, configurações e relatórios globais.
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
                        new Tag().name("Administrador").description("Gestão de perfis e contas administrativas do sistema (teto máximo de 3 administradores ativos)."),
                        new Tag().name("Bolsista").description("Cadastro, consulta paginada, edição de vigência/bolsas e soft-delete de bolsistas."),
                        new Tag().name("Professor").description("Cadastro, consulta e soft-delete de professores coordenadores (restrito a Administradores)."),
                        new Tag().name("Laboratório").description("Gestão de laboratórios de pesquisa, vinculação de coordenadores e controle de ocupação."),
                        new Tag().name("Projeto").description("Gestão de projetos de pesquisa, vinculação de membros e anexação de entregáveis/repositórios."),
                        new Tag().name("Curso").description("Lista de cursos disponíveis para vínculo de bolsistas, com cadastro restrito a Administradores."),
                        new Tag().name("Frequência & Horas").description("Apontamento de horas trabalhadas, resumo mensal, exportação em CSV e emissão de comprovantes em PDF."),
                        new Tag().name("Relatório & Estatísticas").description("Métricas consolidadas de ocupação, carga horária mensal e projetos ativos (exclusivo para perfil ADMIN).")
                ))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido via POST /api/v1/auth/login (retornado no header X-Auth-Token). Use como: Authorization: Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
