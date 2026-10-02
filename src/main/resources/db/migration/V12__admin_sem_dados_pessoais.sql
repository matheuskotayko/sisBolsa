-- ============================================================
-- V12__admin_sem_dados_pessoais.sql
-- O admin do seed e a conta de administracao do sistema, nao uma pessoa:
-- sai o nome ficticio e saem curso, matricula, CPF, telefone, data de
-- nascimento e foto. O e-mail e a senha de acesso continuam os mesmos.
-- ============================================================

UPDATE bolsista
SET nome = 'Admin',
    curso = NULL,
    matricula = NULL,
    cpf = NULL,
    telefone = NULL,
    data_nascimento = NULL,
    foto_url = NULL
WHERE id = 'a1111111-1111-1111-1111-111111111111';

-- a trilha de auditoria da V8 grava o nome de quem agiu; sem isto a
-- demo mostraria acoes de um "Carlos Henrique Alencar" que nao existe mais
UPDATE auditoria
SET usuario_nome = 'Admin'
WHERE usuario_id = 'a1111111-1111-1111-1111-111111111111';
