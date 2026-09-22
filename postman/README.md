# Postman

`SisBolsa-API.postman.json` e a especificacao OpenAPI 3 exportada direto do
`/v3/api-docs` da aplicacao rodando. Nao e uma colecao escrita a mao: e o
mesmo contrato que o springdoc gera a partir das anotacoes `@Operation` e
`@Schema` do codigo, entao nunca fica desatualizada em relacao a API de
verdade — regenerar e so exportar de novo.

## Importar

1. Suba a aplicacao (`docker compose up -d` ou `mvn spring-boot:run`).
2. No Postman: **Import** → arraste `SisBolsa-API.postman.json` (ou aponte
   direto para `http://localhost:8080/v3/api-docs` pela URL) → **Import**.
3. O Postman converte sozinho para colecao, com uma pasta por tag
   (Autenticacao, Bolsistas, Professores, Cursos, Laboratorios, Projetos,
   Frequencia & Horas, Relatorios & Estatisticas, Auditoria) e os corpos de
   requisicao ja preenchidos com os exemplos dos DTOs.

## Autenticacao

A API usa JWT em cookie `httpOnly` (nao Bearer token). Fluxo no Postman:

1. Rode `POST Autenticacao → Autenticar usuario` com um e-mail e senha
   validos (seed: `admin@sisbolsa.com` / `12345678`).
2. O Postman guarda o cookie `Set-Cookie` da resposta no proprio cookie jar
   automaticamente.
3. Toda chamada seguinte para `localhost:8080` na mesma sessao do Postman ja
   sai autenticada — nao precisa copiar token em lugar nenhum.

## Exemplos e o seed

Todo endpoint de criacao sai com um corpo que cria de primeira contra o
banco recem-subido, sem esbarrar no seed do Flyway: professora Juliana
Ferreira, bolsista Ana Pereira (CPF e matricula ineditos), curso Engenharia
de Computacao, e o vinculo do Diego ao projeto do laboratorio de software.
Rodar o mesmo POST duas vezes da 409 - e o `UNIQUE` do banco fazendo o
trabalho dele, nao conflito com o seed.

Dois cuidados:

- **PUT de bolsista e de professor reaproveitam o corpo de criacao.**
  Aplique no registro que voce acabou de criar: em outro, o e-mail e o CPF
  do exemplo ja pertencem a esse registro novo e voltam 409. PUT substitui
  o recurso inteiro - campo omitido fica vazio; so a senha em branco e
  preservada.
- **`PATCH /auth/perfil` altera quem esta logado.** O exemplo e o proprio
  admin do seed com a mesma senha, entao so a bio muda e a sessao continua
  valendo.

## Base URL

A colecao vem com `http://localhost:8080` fixado (e o `server` declarado no
OpenAPI). Para apontar para outro host, edite a variavel de URL base que o
Postman cria na importacao.

## Regenerar

```bash
curl -s http://localhost:8080/v3/api-docs | python3 -m json.tool > postman/SisBolsa-API.postman.json
```

---

## Casos Negativos

`SisBolsa-Casos-Negativos.postman.json` e uma colecao de verdade, escrita a
mao (nao regenerada) demonstrando os erros da API: cada excecao de dominio
(`RecursoNaoEncontradoException`, `PermissaoNegadaException`,
`LimiteAdminsAtingidoException`, `CredenciaisInvalidasException`,
`ContaBloqueadaException`), a validacao via Bean Validation (JSR 380) e um
`IllegalArgumentException` de regra de negocio simples - cada pasta com o
status HTTP e a mensagem de erro reais que o `ApiExceptionHandler` devolve,
mais um `pm.test` conferindo os dois.

### Como rodar

1. Suba a aplicacao com o seed de apresentacao intacto (nenhum dado extra
   inserido manualmente).
2. Importe `SisBolsa-Casos-Negativos.postman.json` no Postman.
3. Rode a colecao inteira em ordem, via **Collection Runner** (ou uma
   pasta de cada vez, na ordem numerada) - o cookie de sessao troca de
   admin pra bolsista na pasta 5, entao a ordem importa.

A pasta `4. 409` cria 2 administradores extras pra forcar o limite e os
desativa (soft delete) ao final. Como o soft delete mantem a linha - e o
e-mail continua ocupado -, os e-mails levam um sufixo gerado a cada
execucao: a colecao roda quantas vezes quiser sem deixar admin ativo
sobrando. Ela conta com o limite de 3 admins partindo de 1 (o do seed),
entao rode antes do `POST /auth/cadastro-admin` da colecao principal - ou
num banco recem-subido. A pasta `7. 429` usa um e-mail que nao existe no sistema, entao
nao bloqueia nenhuma conta real.

O request `Bolsista tenta cadastrar usuario` (pasta `5. 403`) tem um
pre-request script que reloga como bolsista sozinho antes de disparar -
entao mesmo clicando soh nele, isolado, sem rodar `Login como Bolsista`
antes, o teste continua valendo. Sem isso, clicar direto nele com uma
sessao de admin ainda ativa (do Setup) faz o proprio admin criar o
usuario com sucesso (201) em vez de barrar (403) - nao e falha de
seguranca, e so o cookie jar do Postman mantendo a sessao antiga.
