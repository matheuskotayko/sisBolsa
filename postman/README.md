# SisBolsa - Postman Collections

## Importar Collection Atualizada

A collection foi migrada para **Bearer Token JWT** (removido cookie HTTP-only).

### Método 1: Importar do OpenAPI/Swagger (Recomendado)

1. **Inicie a aplicação:**
   ```bash
   docker compose up -d  # ou mvn spring-boot:run
   ```

2. **Abra o Postman** → Clique em **Import**

3. **Cole a URL do OpenAPI:**
   ```
   http://localhost:8080/v3/api-docs
   ```

4. **Configure o Workspace** (quando perguntar)

5. **Pronto!** Todas as requests vêm com Bearer token pre-configurado

### Método 2: Importar Arquivo (Desatualizado)

Os arquivos `.postman.json` nesta pasta podem estar desatualizados.
Use o Método 1 para garantir versão atual.

---

## Usar Bearer Token

### Login e Copiar Token

1. **POST** `/api/v1/auth/login`
   ```json
   {
     "email": "admin@sisbolsa.com",
     "senha": "12345678"
   }
   ```

2. **Response headers** conterá:
   ```
   X-Auth-Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

3. **Copie o token** do header `X-Auth-Token`

### Configurar em Outras Requests

**Authorization** → Type: `Bearer Token` → Token: `<cole aqui>`

Ou defina como **variável global**:
- Clique **Environments** (canto superior direito)
- Adicione variable: `token = eyJhbGci...`
- Use em requests: `Authorization: Bearer {{token}}`

---

## Credenciais Padrão

| Email | Senha | Perfil |
|-------|-------|--------|
| `admin@sisbolsa.com` | `12345678` | ADMIN |

---

## Autenticação

Todas as requests `/api/v1/*` requerem:
```
Authorization: Bearer <token_do_login>
```

Tokens expiram conforme configuração `app.jwt.expiracao-minutos` (default: 240 min).
