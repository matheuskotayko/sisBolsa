# SisBolsa - Postman Collections

## Coleções Disponíveis

Esta pasta contém as coleções do Postman para consumo e teste do SisBolsa:

1. **`SisBolsa-API.postman.json`**: Especificação completa da API sincronizada com o OpenAPI 3.1.0 e compatível com o Postman. Contém todas as rotas no **singular** (`/api/v1/bolsista`, `/api/v1/professor`, `/api/v1/administrador`, etc.), esquemas de dados atualizados e exemplos com **identificadores públicos prefixados** (`adm_...`, `prf_...`, `bol_...`, `lab_...`, `prj_...`, `cur_...`, `frq_...`).
2. **`SisBolsa-Casos-Negativos.postman.json`**: Coleção automatizada no formato Postman Collection v2.1.0 com testes cobrindo a matriz de exceções e erros da API:
   - **400**: Bean Validation (JSR-380) e validação de senhas divergentes
   - **401**: Credenciais inválidas e requisições sem token de autenticação
   - **403**: Permissões de RBAC (ex: bolsista tentando cadastrar usuário) e restrições do Spring Security em `/api/v1/relatorio/**`
   - **404**: Busca por ID público inexistente (`/api/v1/projeto/prj_00000000000000000000`)
   - **409**: Limite máximo de administradores ativos atingido e violação de chave única de e-mail
   - **429**: Bloqueio temporário por rate limiting de tentativas de login incorretas

---

## Importar no Postman

### Método 1: Importar Arquivos Locais (Recomendado)

1. No Postman, clique em **Import** (canto superior esquerdo).
2. Arraste ou selecione os arquivos `SisBolsa-API.postman.json` e/ou `SisBolsa-Casos-Negativos.postman.json`.
3. Os endpoints e variáveis de ambiente/coleção (`baseUrl`, `token`) serão configurados automaticamente.

### Método 2: Importar via URL OpenAPI

Com a aplicação em execução:
1. Clique em **Import** no Postman.
2. Insira a URL: `http://localhost:8080/v3/api-docs`
3. A documentação OpenAPI 3.1.0 será importada gerando as requisições atualizadas.

---

## Autenticação com Bearer Token (JWT)

A API utiliza autenticação **Stateless via Bearer Token JWT** no header `Authorization`.

### Login e Obtenção do Token

1. Execute a requisição **POST** `/api/v1/auth/login`:
   ```json
   {
     "email": "admin@sisbolsa.com",
     "senha": "12345678"
   }
   ```
2. O token JWT é retornado no header de resposta:
   ```http
   X-Auth-Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
3. Na coleção de **Casos Negativos**, o script de teste de login armazena automaticamente o token na variável `{{token}}` da coleção.
4. Para requisições manuais avulsas, envie o header:
   ```http
   Authorization: Bearer <seu_token_jwt>
   ```

---

## Credenciais Padrão (Seed de Apresentação)

| Perfil | Email | Senha | Identificador / Regras |
|--------|-------|-------|------------------------|
| **ADMIN** | `admin@sisbolsa.com` | `12345678` | Acesso global, relatórios e gestão de perfis |
| **PROFESSOR** | `fernando.silva@prof.sisbolsa.com` | `12345678` | Coordenador de laboratórios e projetos |
| **BOLSISTA** | `diego.almeida@aluno.sisbolsa.com` | `12345678` | Apontamento de horas e visualização do lab |

---

## Identificadores Públicos (Public IDs)

Todos os endpoints que aceitam identificadores utilizam os identificadores públicos prefixados:
- Administradores: `adm_...`
- Professores: `prf_...`
- Bolsistas: `bol_...`
- Laboratórios: `lab_...`
- Projetos: `prj_...`
- Cursos: `cur_...`
- Frequências: `frq_...`
- Usuários (Auth): `usr_...`
