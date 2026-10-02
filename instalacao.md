# Guia de Instalacao e Execucao — SisBolsa

## Pre-requisitos

| Ferramenta | Versao Recomendada | Finalidade |
|---|---|---|
| **Docker & Docker Compose** | Qualquer versao recente | Execucao completa em containers (banco + aplicacao) |
| **Java JDK** | 21 | Execucao local do backend (opcional caso use Docker) |
| **Maven** | 3.9+ | Build local do backend |

---

## 1. Execucao Rapida via Docker (Recomendado)

O Docker Compose sobe automaticamente o banco PostgreSQL e a aplicacao (API + Swagger).

1. Clone o repositorio e acerte o diretorio:
   ```bash
   git clone <url-do-repositorio>
   cd trabalho-finalp-poow1
   ```

2. Inicie os containers:
   ```bash
   docker compose up -d --build
   ```

3. Swagger UI / Documentacao da API:
   - URL: **http://localhost:8080/swagger-ui.html**

### Comandos Uteis do Docker
```bash
# Ver logs em tempo real
docker compose logs -f

# Parar a aplicacao mantendo os dados
docker compose down

# Parar e resetar o banco de dados do zero
docker compose down -v
```

---

## 2. Execucao em Modo de Desenvolvimento

Caso deseje rodar o backend localmente (fora do container):

### Passo 1: Subir o PostgreSQL
```bash
docker compose up -d db
```

### Passo 2: Subir a aplicacao Spring Boot
```bash
mvn spring-boot:run
```

---

## 3. Credenciais Iniciais de Acesso

O banco e populado automaticamente via Flyway com as seguintes contas:

### Administrador
- **E-mail:** `admin@sisbolsa.com`
- **Senha:** `12345678`

### Professores Coordenadores
- `roberto.mendes@sisbolsa.com` / `12345678` (Lab. Desenvolvimento de Software)
- `carla.souza@sisbolsa.com` / `12345678` (Lab. Inteligencia Artificial e Dados)

### Bolsistas (Exemplos)
- `lucas.oliveira@aluno.sisbolsa.com` / `12345678`
- `mariana.santos@aluno.sisbolsa.com` / `12345678`
- `diego.almeida@aluno.sisbolsa.com` / `12345678`

---

## 4. Executando os Testes Automatizados

A aplicacao possui 100 testes automatizados (unitarios, seguranca, services e controllers mockados) que rodam sem necessidade de banco ativo:

```bash
mvn test
```
