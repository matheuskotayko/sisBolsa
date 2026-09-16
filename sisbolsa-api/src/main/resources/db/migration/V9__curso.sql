-- ============================================================
-- V9__curso.sql
-- Lista de cursos gerenciavel: comeca fixa (dropdown no cadastro de
-- bolsista) mas admin pode adicionar novos em tempo de execucao.
-- ============================================================

CREATE TABLE curso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO curso (nome) VALUES
    ('Sistemas para Internet'),
    ('Sistemas de Informação'),
    ('Ciência da Computação'),
    ('Redes de Computadores');
