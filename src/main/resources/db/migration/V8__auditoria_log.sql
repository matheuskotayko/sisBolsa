-- ============================================================
-- V8__auditoria_log.sql
-- Tabela de trilha de auditoria e logs de atividades do sistema
-- ============================================================

CREATE TABLE IF NOT EXISTS auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID,
    usuario_nome VARCHAR(255),
    acao VARCHAR(100) NOT NULL,
    entidade VARCHAR(50) NOT NULL,
    detalhes TEXT,
    ip_origem VARCHAR(100),
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditoria_data_hora ON auditoria(data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_entidade ON auditoria(entidade);

-- Inserção de eventos de auditoria de exemplo
INSERT INTO auditoria (id, usuario_id, usuario_nome, acao, entidade, detalhes, ip_origem, data_hora) VALUES
('aa111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 'Carlos Henrique Alencar', 'LOGIN', 'AUTH', 'Login efetuado com sucesso no painel administrativo.', '127.0.0.1', CURRENT_TIMESTAMP - INTERVAL '3 days'),
('aa222222-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', 'Carlos Henrique Alencar', 'CRIAR_LABORATORIO', 'LABORATORIO', 'Cadastro do Lab de Desenvolvimento de Software coordenado pelo Dr. Roberto Mendes.', '127.0.0.1', CURRENT_TIMESTAMP - INTERVAL '2 days'),
('aa333333-3333-3333-3333-333333333333', 'b1111111-1111-1111-1111-111111111111', 'Dr. Roberto Mendes', 'CRIAR_PROJETO', 'PROJETO', 'Criado projeto "Sistema Web de Gestão Acadêmica".', '192.168.1.10', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('aa444444-4444-4444-4444-444444444444', 'b1111111-1111-1111-1111-111111111111', 'Dr. Roberto Mendes', 'VINCULAR_BOLSISTA', 'PROJETO', 'Bolsista Lucas Oliveira vinculado ao projeto "Sistema Web de Gestão Acadêmica".', '192.168.1.10', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('aa555555-5555-5555-5555-555555555555', 'd1111111-1111-1111-1111-111111111111', 'Lucas Oliveira', 'REGISTRAR_FREQUENCIA', 'FREQUENCIA', 'Apontamento de 4.0h de pesquisa com link de PR no GitHub.', '192.168.1.15', CURRENT_TIMESTAMP - INTERVAL '2 hours');
