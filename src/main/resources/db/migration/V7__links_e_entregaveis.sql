-- ============================================================
-- V7__links_e_entregaveis.sql
-- Adiciona links externos e entregaveis em projetos e frequencias
-- ============================================================

ALTER TABLE projeto
ADD COLUMN IF NOT EXISTS link_repositorio VARCHAR(500),
ADD COLUMN IF NOT EXISTS link_documentacao VARCHAR(500);

ALTER TABLE frequencia
ADD COLUMN IF NOT EXISTS link_comprovante VARCHAR(500);

-- Atualiza projetos com links de exemplo
UPDATE projeto
SET link_repositorio = 'https://github.com/exemplo/sistema-web-gestao',
    link_documentacao = 'https://overleaf.com/read/exemplo-artigo-academico'
WHERE id = 'e1111111-1111-1111-1111-111111111111';

UPDATE projeto
SET link_repositorio = 'https://github.com/exemplo/modelagem-ml-pesquisa',
    link_documentacao = 'https://notion.so/exemplo-documentacao-dados'
WHERE id = 'e2222222-2222-2222-2222-222222222222';

-- Atualiza frequencias com links de entregaveis de exemplo
UPDATE frequencia
SET link_comprovante = 'https://github.com/exemplo/sistema-web-gestao/pull/12'
WHERE id = 'f1111111-1111-1111-1111-111111111111';

UPDATE frequencia
SET link_comprovante = 'https://github.com/exemplo/sistema-web-gestao/commit/a1b2c3d'
WHERE id = 'f2222222-2222-2222-2222-222222222222';
