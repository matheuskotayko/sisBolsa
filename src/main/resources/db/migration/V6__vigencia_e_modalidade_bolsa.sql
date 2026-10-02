-- ============================================================
-- V6__vigencia_e_modalidade_bolsa.sql
-- Adiciona campos de vigencia, modalidade e valor da bolsa
-- ============================================================

ALTER TABLE bolsista
ADD COLUMN IF NOT EXISTS modalidade_bolsa VARCHAR(50),
ADD COLUMN IF NOT EXISTS valor_bolsa NUMERIC(10,2),
ADD COLUMN IF NOT EXISTS data_inicio_bolsa DATE,
ADD COLUMN IF NOT EXISTS data_fim_bolsa DATE;

-- Atualiza os bolsistas existentes no seed com modalidades e valores de exemplo
UPDATE bolsista
SET modalidade_bolsa = 'PIBIC',
    valor_bolsa = 700.00,
    data_inicio_bolsa = CURRENT_DATE - INTERVAL '6 months',
    data_fim_bolsa = CURRENT_DATE + INTERVAL '6 months'
WHERE email = 'lucas.oliveira@aluno.sisbolsa.com';

UPDATE bolsista
SET modalidade_bolsa = 'PIBITI',
    valor_bolsa = 700.00,
    data_inicio_bolsa = CURRENT_DATE - INTERVAL '4 months',
    data_fim_bolsa = CURRENT_DATE + INTERVAL '8 months'
WHERE email = 'mariana.santos@aluno.sisbolsa.com';

UPDATE bolsista
SET modalidade_bolsa = 'EXTENSAO',
    valor_bolsa = 500.00,
    data_inicio_bolsa = CURRENT_DATE - INTERVAL '2 months',
    data_fim_bolsa = CURRENT_DATE + INTERVAL '10 months'
WHERE email = 'diego.almeida@aluno.sisbolsa.com';

UPDATE bolsista
SET modalidade_bolsa = 'INSTITUCIONAL',
    valor_bolsa = 700.00,
    data_inicio_bolsa = CURRENT_DATE - INTERVAL '5 months',
    data_fim_bolsa = CURRENT_DATE + INTERVAL '7 months'
WHERE email = 'thiago.rocha@aluno.sisbolsa.com';
