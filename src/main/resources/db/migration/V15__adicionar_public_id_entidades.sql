-- ============================================================
-- V15__adicionar_public_id_entidades.sql
-- Adiciona identificadores públicos (public_id) prefixados no estilo Stripe
-- para todas as entidades do sistema, desacoplando os IDs públicos das PKs UUID.
-- ============================================================

-- 1. Adiciona coluna public_id em todas as tabelas
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE administrador ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE professor ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE bolsista ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE laboratorio ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE projeto ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE curso ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);
ALTER TABLE frequencia ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);

-- 2. Preenche public_id para registros já existentes com hash determinístico seguro
UPDATE usuario SET public_id = 'usr_' || substr(md5(id::text || 'usr'), 1, 20) WHERE public_id IS NULL;
UPDATE administrador SET public_id = 'adm_' || substr(md5(id::text || 'adm'), 1, 20) WHERE public_id IS NULL;
UPDATE professor SET public_id = 'prf_' || substr(md5(id::text || 'prf'), 1, 20) WHERE public_id IS NULL;
UPDATE bolsista SET public_id = 'bol_' || substr(md5(id::text || 'bol'), 1, 20) WHERE public_id IS NULL;
UPDATE laboratorio SET public_id = 'lab_' || substr(md5(id::text || 'lab'), 1, 20) WHERE public_id IS NULL;
UPDATE projeto SET public_id = 'prj_' || substr(md5(id::text || 'prj'), 1, 20) WHERE public_id IS NULL;
UPDATE curso SET public_id = 'cur_' || substr(md5(id::text || 'cur'), 1, 20) WHERE public_id IS NULL;
UPDATE frequencia SET public_id = 'frq_' || substr(md5(id::text || 'frq'), 1, 20) WHERE public_id IS NULL;

-- 3. Aplica restrição NOT NULL após preenchimento dos dados existentes
ALTER TABLE usuario ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE administrador ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE professor ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE bolsista ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE laboratorio ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE projeto ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE curso ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE frequencia ALTER COLUMN public_id SET NOT NULL;

-- 4. Adiciona restrições UNIQUE de integridade relacional
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_usuario_public_id') THEN
        ALTER TABLE usuario ADD CONSTRAINT uk_usuario_public_id UNIQUE (public_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_administrador_public_id') THEN
        ALTER TABLE administrador ADD CONSTRAINT uk_administrador_public_id UNIQUE (public_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_professor_public_id') THEN
        ALTER TABLE professor ADD CONSTRAINT uk_professor_public_id UNIQUE (public_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_bolsista_public_id') THEN
        ALTER TABLE bolsista ADD CONSTRAINT uk_bolsista_public_id UNIQUE (public_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_laboratorio_public_id') THEN
        ALTER TABLE laboratorio ADD CONSTRAINT uk_laboratorio_public_id UNIQUE (public_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_projeto_public_id') THEN
        ALTER TABLE projeto ADD CONSTRAINT uk_projeto_public_id UNIQUE (public_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_curso_public_id') THEN
        ALTER TABLE curso ADD CONSTRAINT uk_curso_public_id UNIQUE (public_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_frequencia_public_id') THEN
        ALTER TABLE frequencia ADD CONSTRAINT uk_frequencia_public_id UNIQUE (public_id);
    END IF;
END $$;
