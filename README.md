# SisBolsa — API de Gestao de Bolsistas e Laboratorios
![Preview](./docs/images/preview.png)

API RESTful para gestao integrada de bolsistas de pesquisa, professores orientadores, laboratorios, projetos academicos, controle de frequencia e trilha de auditoria.

Construida em **Spring Boot 4 / Java 21**, com persistencia via **Spring Data JPA** e documentacao automatica via **Swagger/OpenAPI**.

> Esta branch (`main`) contem somente o backend. O frontend React/Vite que consome esta API mora na branch `fullstack`.

---

## Tecnologias

| Camada | Tecnologias |
|---|---|
| **Backend** | Spring Boot 4.0.6, Java 21, Spring Data JPA, Hibernate |
| **Seguranca** | Spring Security, JWT Bearer Token, BCrypt, Rate Limiting anti-bruteforce |
| **Banco de Dados** | PostgreSQL 15, Flyway Migration |
| **Documentacao** | OpenAPI 3 / Swagger UI (springdoc 3.1.0) |
| **Relatorios** | LibrePDF / OpenPDF (geracao nativa de comprovantes em PDF) |
| **Infraestrutura** | Docker, Docker Compose (Build multi-stage) |

---

## Principais Funcionalidades

1. **Gestao de Bolsistas e Vinculos:**
   - Cadastro completo de bolsistas, vigencia de bolsa (inicio/termino), modalidades (`PIBIC`, `PIBITI`, `Extensao`, `Monitoria`, etc.) e valor mensal da bolsa (R$).
   - Controle de status de vencimento da bolsa com avisos visuais.
   - Atribuicao de cargos academicos e lotacao em laboratorios.

2. **Laboratorios & Controle de Capacidade:**
   - Cadastro de laboratorios de pesquisa vinculados a professores coordenadores.
   - Indicadores visuais de percentual de ocupacao em tempo real (com avisos de lotacao).

3. **Projetos de Pesquisa & Entregaveis:**
   - Gestao de projetos por laboratorio e vinculacao de membros da equipe.
   - Links diretos para entregaveis externos (repositorios GitHub/GitLab, artigos Overleaf e documentacoes).

4. **Controle de Frequencia & Horas:**
   - Apontamento de horas trabalhadas com descricao de tarefas e link de entregaveis/PRs.
   - Filtros por intervalo de datas e exportacao de relatorios em formato CSV.
   - **Emissao de Comprovante Oficial em PDF:** Geracao de documento institucional A4 com resumo do bolsista, tabela zebrada de apontamentos e campos para assinatura do bolsista e coordenador.

5. **Relatorios & Estatisticas (Admin):**
   - Graficos interativos com Recharts (Ocupacao dos labs, distribuicao de cargos, horas trabalhadas no mes e projetos por laboratorio).
   - Alternancia rapida entre visualizacao grafica e tabular.

6. **Seguranca & Controle de Acesso (RBAC):**
   - Tres perfis bem definidos: `ADMIN`, `PROFESSOR` e `BOLSISTA`.
   - **Rate Limiting no Login:** Bloqueio temporario de 5 minutos apos 5 falhas consecutivas de autenticacao.
   - **Fluxo de "Esqueci a Senha":** Recuperacao segura de senha atraves de codigo de verificacao temporario.
   - **Alteracao de Perfil:** Endpoint dedicado para troca de dados cadastrais e senha exigindo validacao da senha atual.

---

## Perfis de Acesso

- **`ADMIN`:** Acesso irrestrito a todos os laboratorios, bolsistas, projetos e relatorios globais.
- **`PROFESSOR`:** Gerenciamento dos laboratorios que coordena, seus projetos associados e bolsistas vinculados.
- **`BOLSISTA`:** Apontamento e edicao de frequencia propria, visualizacao de equipe/projetos do laboratorio e emissao de comprovantes em PDF.

---

## Modelagem do Banco de Dados

Modelagem relacional e conceitual projetada na ferramenta **brModelo**, refletindo as regras de negocio, entidades e integridade referencial do sistema.

### Modelo Conceitual (MER)
Representacao das entidades (`PROFESSOR`, `LABORATORIO`, `BOLSISTA`, `PROJETO` e `FREQUENCIA`), seus atributos e relacionamentos de cardinalidade (1:N e N:N).

![Modelo Conceitual (MER)](./docs/images/diagrama-er.png)

### Modelo Logico (DER)
Esquema relacional normalizado contendo as definicoes de tabelas, tipos de dados, chaves primarias (PK), chaves estrangeiras (FK) e a tabela associativa `bolsista_projeto`.

![Modelo Logico (DER)](./docs/images/modelo-logico.png)

---

## Como Rodar

### Com Docker (Recomendado)

```bash
docker compose up -d --build
```

A API sobe em: **[http://localhost:8080](http://localhost:8080)**

### Desenvolvimento Local

```bash
# 1. Subir apenas o banco de dados
docker compose up -d db

# 2. Rodar a aplicacao Spring Boot
mvn spring-boot:run
```

### Acesso Inicial

```
E-mail: admin@sisbolsa.com
Senha:  12345678
```

---

## Documentacao da API (Swagger)

Com a aplicacao rodando, acesse a documentacao interativa:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Postman:** [postman/SisBolsa-API.postman.json](postman/SisBolsa-API.postman.json) — importe direto no Postman (organiza sozinho em uma pasta por tag). Instrucoes em [postman/README.md](postman/README.md).

---

## Testes Automatizados

A suite conta com 100 testes automatizados (unitarios e de contexto com mockMvc) que nao dependem de banco de dados externo:

```bash
mvn test
```
