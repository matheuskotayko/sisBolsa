<h1 align="center">SisBolsa</h1>

> API REST para a coordenação de bolsistas de pesquisa: cadastre bolsistas, laboratórios e projetos, registre as horas trabalhadas e emita o comprovante de frequência em PDF, pronto para assinatura.

Projeto da disciplina de Programação Orientada a Objetos para Web II (UFSM). Spring Boot 4, Java 21 e PostgreSQL.

## Por que

- **Cada perfil enxerga só o que é seu.** `ADMIN`, `PROFESSOR` e `BOLSISTA` têm papéis distintos: o professor mexe no laboratório que coordena e o bolsista só no próprio cadastro e nas próprias horas.
- **Login protegido contra força bruta.** Cinco falhas seguidas bloqueiam o e-mail por 5 minutos (`LoginService`).
- **Banco versionado.** O schema é dono do Flyway (`db/migration`, V1 a V15) e o Hibernate só valida (`ddl-auto: validate`), então entidade e tabela não saem de sincronia sem alguém notar.
- **Comprovante oficial em PDF.** O endpoint `/frequencia/comprovante-pdf` gera um A4 com o resumo do bolsista, as horas apontadas e os campos de assinatura.
- **API documentada e testável.** Swagger UI, OpenAPI e duas coleções Postman, uma delas só com casos negativos (400, 401, 403, 404, 409 e 429).

## Como funciona

```
cliente ──► JwtAuthenticationFilter ──► Controller ──► Service ──► Repository ──► PostgreSQL
              (Bearer token)            (valida DTO)   (regras e     (Spring Data    (schema via
                                                        permissões)   JPA)            Flyway)
```

O login passa pelo `AuthenticationManager` do Spring Security, que consulta o `AutenticacaoService` (`UserDetailsService`) e compara a senha com BCrypt. A resposta traz o JWT no header `Authorization: Bearer ...`. A API é stateless: cada requisição é autenticada só pelo token. Regras que dependem do dado (por exemplo, "professor só edita o próprio laboratório") ficam nos services. Erros saem em JSON `{"mensagem": ...}` pelo `ApiExceptionHandler`, e erros de validação saem como lista `[{campo, mensagem}]`.

## Recursos da API

Tudo sob `/api/v1`, exceto o que está marcado como público.

| Recurso | Caminho | O que faz |
|---|---|---|
| Autenticação | `/auth` | `login` (público), `logout`, `me`, `perfil`, `cadastro-admin` e recuperação de senha por código (públicos) |
| Bolsistas | `/bolsista` | CRUD, cargos, modalidades, exportação CSV, projetos do bolsista |
| Professores | `/professor` | CRUD (somente `ADMIN`) |
| Administradores | `/administrador` | CRUD (somente `ADMIN`, limite de 3 ativos) |
| Laboratórios | `/laboratorio` | CRUD, bolsistas e projetos do laboratório, capacidade |
| Projetos | `/projeto` | CRUD, vínculo e desvínculo de membros |
| Frequência | `/frequencia` | apontamento de horas, resumo, exportação CSV, comprovante em PDF |
| Cursos | `/curso` | listagem e cadastro |
| Relatórios | `/relatorio` | ocupação dos laboratórios, horas no mês, bolsistas por cargo, projetos por laboratório (somente `ADMIN`) |

As listagens aceitam paginação do Spring (`page`, `size`, `sort`): 10 itens por padrão e no máximo 200.

## Início rápido

Pré-requisito: Docker com Compose.

```bash
docker compose up -d --build
```

A API sobe em `http://localhost:8080` e o Swagger UI em `http://localhost:8080/swagger-ui.html`. O Flyway já cria o schema e popula dados de exemplo. Entre com o administrador:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@sisbolsa.com","senha":"12345678"}'
```

O token vem no header `Authorization`. Para usá-lo, mande `Authorization: Bearer <token>` nas demais chamadas.

Contas de exemplo do seed (todas com senha `12345678`):

| Perfil | E-mail |
|---|---|
| `ADMIN` | `admin@sisbolsa.com` |
| `PROFESSOR` | `roberto.mendes@sisbolsa.com`, `carla.souza@sisbolsa.com` |
| `BOLSISTA` | `lucas.oliveira@aluno.sisbolsa.com`, `mariana.santos@aluno.sisbolsa.com`, `diego.almeida@aluno.sisbolsa.com` |

Para parar, use `docker compose down`. Acrescente `-v` para apagar também os dados do banco.

### Desenvolvimento local

Requer JDK 21. Suba só o banco e rode a aplicação pelo wrapper do Maven:

```bash
docker compose up -d db
./mvnw spring-boot:run
```

## Configuração

Todas as variáveis são opcionais; os valores padrão servem para desenvolvimento.

| Variável | Padrão | Significado |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5436` | Porta do PostgreSQL (o compose publica o banco em 5436) |
| `DB_NAME` | `cadastroBolsista` | Nome do banco |
| `DB_USER` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `1234` | Senha do banco |
| `JWT_SECRET` | valor de exemplo no `application.yml` | Segredo de assinatura do JWT (mínimo 32 bytes) |
| `JWT_EXPIRACAO_MINUTOS` | `120` | Validade do token |

## Segurança

- JWT assinado (HMAC), sem sessão de servidor e sem CSRF por ser stateless; senhas com BCrypt.
- Rotas públicas: login, cadastro de administrador e recuperação de senha. Todo o resto de `/api/**` exige token e responde 401 ou 403 em JSON.
- Bloqueio temporário de login após 5 falhas consecutivas, com aviso das tentativas restantes.
- Os valores padrão de `JWT_SECRET` e `DB_PASSWORD`, e as contas do seed, existem só para desenvolvimento. Troque todos antes de expor a API.

## Estrutura do projeto

```
src/main/java/dev/matheus/cadastroBolsistas/
├── controller/   endpoints REST (só delegam ao service)
├── service/      regras de negócio, permissões, login, PDF
├── repository/   Spring Data JPA, Specifications de filtro
├── model/        entidades JPA
├── dto/          request/ (entrada validada) e response/ (saída)
├── exceptions/   exceções de domínio e ApiExceptionHandler
├── infra/        SecurityConfig e security/ (TokenServiceJWT, JwtAuthenticationFilter)
└── common/       utilitários (CSV, ids públicos, OpenAPI)
src/main/resources/db/migration/   migrations Flyway (V1..V15)
```

O modelo de dados está em [`docs/images/er-model.png`](docs/images/er-model.png) (entidade-relacionamento) e [`docs/images/model.png`](docs/images/model.png) (modelo lógico), feitos para a modelagem do projeto.

## Documentação

- Swagger UI: `http://localhost:8080/swagger-ui.html`; OpenAPI JSON: `http://localhost:8080/v3/api-docs`.
- Coleções Postman e instruções de importação: [`postman/`](postman/README.md).
- Apresentação do projeto: [`docs/SisBolsa_Apresentacao.pdf`](docs/SisBolsa_Apresentacao.pdf).

## Testes

A suíte tem 215 testes (unitários, controllers com MockMvc e consultas reais de repositório). Os de repositório precisam do banco no ar:

```bash
docker compose up -d db
./mvnw test
```
