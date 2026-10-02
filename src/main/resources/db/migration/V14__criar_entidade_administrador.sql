-- ============================================================
-- V14__criar_entidade_administrador.sql
-- Desacopla as credenciais e identidade na tabela usuario e
-- cria a tabela de perfil administrador, transferindo o admin
-- do seed para fora da tabela bolsista.
-- ============================================================

-- 1. Cria a tabela usuario com chave primaria UUID
CREATE TABLE IF NOT EXISTS usuario (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    tipo_usuario VARCHAR(20) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    foto_url VARCHAR(255),
    bio TEXT
);

-- 2. Migra os usuarios de professor para a tabela usuario
INSERT INTO usuario (id, nome, email, senha, tipo_usuario, ativo, foto_url, bio)
SELECT id, nome, email, senha, 'PROFESSOR', ativo, foto_url, bio FROM professor
ON CONFLICT (id) DO NOTHING;

-- 3. Migra os bolsistas e administradores para a tabela usuario
INSERT INTO usuario (id, nome, email, senha, tipo_usuario, ativo, foto_url, bio)
SELECT id, nome, email, senha, tipo_usuario, ativo, foto_url, bio FROM bolsista
ON CONFLICT (id) DO NOTHING;

-- 4. Cria a tabela de perfil administrador
CREATE TABLE IF NOT EXISTS administrador (
    id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    cargo VARCHAR(100) DEFAULT 'Administrador Geral',
    telefone VARCHAR(20)
);

-- 5. Insere os administradores existentes na tabela administrador
INSERT INTO administrador (id, cargo)
SELECT id, 'Administrador do Sistema' FROM bolsista WHERE tipo_usuario = 'ADMIN'
ON CONFLICT (id) DO NOTHING;

-- 6. Remove os administradores da tabela bolsista
DELETE FROM bolsista WHERE tipo_usuario = 'ADMIN';

-- 7. Garante constraints de integridade referencial com usuario
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_bolsista_usuario') THEN
        ALTER TABLE bolsista ADD CONSTRAINT fk_bolsista_usuario FOREIGN KEY (id) REFERENCES usuario(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_professor_usuario') THEN
        ALTER TABLE professor ADD CONSTRAINT fk_professor_usuario FOREIGN KEY (id) REFERENCES usuario(id) ON DELETE CASCADE;
    END IF;
END $$;
