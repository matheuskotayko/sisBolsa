-- ============================================================
-- V5__migrar_para_uuid.sql
-- Migracao das chaves primarias e estrangeiras de Integer para UUID
-- ============================================================

DROP TABLE IF EXISTS frequencia CASCADE;
DROP TABLE IF EXISTS bolsista_projeto CASCADE;
DROP TABLE IF EXISTS projeto CASCADE;
DROP TABLE IF EXISTS bolsista CASCADE;
DROP TABLE IF EXISTS laboratorio CASCADE;
DROP TABLE IF EXISTS professor CASCADE;

CREATE TABLE professor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    tipo_usuario VARCHAR(20) NOT NULL DEFAULT 'PROFESSOR',
    ativo BOOLEAN DEFAULT TRUE,
    foto_url VARCHAR(255),
    bio TEXT
);

CREATE TABLE laboratorio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    area_pesquisa VARCHAR(255),
    status VARCHAR(50) DEFAULT 'Ativo',
    capacidade INTEGER DEFAULT 10,
    coordenador_id UUID REFERENCES professor(id),
    ativo BOOLEAN DEFAULT TRUE
);

CREATE TABLE bolsista (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    data_nascimento DATE,
    curso VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    matricula VARCHAR(50),
    cpf VARCHAR(20),
    telefone VARCHAR(20),
    ativo BOOLEAN DEFAULT TRUE,
    laboratorio_id UUID REFERENCES laboratorio(id),
    tipo_usuario VARCHAR(20) DEFAULT 'BOLSISTA',
    foto_url VARCHAR(255),
    cargo VARCHAR(50),
    bio TEXT
);

CREATE TABLE projeto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    laboratorio_id UUID REFERENCES laboratorio(id),
    ativo BOOLEAN DEFAULT TRUE
);

CREATE TABLE bolsista_projeto (
    bolsista_id UUID REFERENCES bolsista(id) ON DELETE CASCADE,
    projeto_id UUID REFERENCES projeto(id) ON DELETE CASCADE,
    PRIMARY KEY (bolsista_id, projeto_id)
);

CREATE TABLE frequencia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bolsista_id UUID REFERENCES bolsista(id) ON DELETE CASCADE,
    data DATE NOT NULL,
    horas_trabalhadas DOUBLE PRECISION NOT NULL,
    descricao TEXT,
    ativo BOOLEAN DEFAULT TRUE
);

-- ============================================================
-- Seed de Apresentacao em UUID
-- ============================================================
INSERT INTO professor (id, nome, email, senha, tipo_usuario, ativo, foto_url, bio) VALUES
('b1111111-1111-1111-1111-111111111111', 'Dr. Roberto Mendes', 'roberto.mendes@sisbolsa.com', '$2a$10$bTT.MiXD1zXSLvIxrMQqV.YR2nTjqkSpwD6P3Cjn3XyZCamHk2BO2', 'PROFESSOR', true, 'https://i.pravatar.cc/150?img=11', NULL),
('b2222222-2222-2222-2222-222222222222', 'Dra. Carla Souza',   'carla.souza@sisbolsa.com',   '$2a$10$bTT.MiXD1zXSLvIxrMQqV.YR2nTjqkSpwD6P3Cjn3XyZCamHk2BO2', 'PROFESSOR', true, 'https://i.pravatar.cc/150?img=49', NULL);

INSERT INTO laboratorio (id, nome, area_pesquisa, status, capacidade, coordenador_id, ativo) VALUES
('c1111111-1111-1111-1111-111111111111', 'Lab de Desenvolvimento de Software',      'Ciência da Computação', 'Ativo', 8, 'b1111111-1111-1111-1111-111111111111', true),
('c2222222-2222-2222-2222-222222222222', 'Lab de Inteligência Artificial e Dados', 'Ciência de Dados',      'Ativo', 6, 'b2222222-2222-2222-2222-222222222222', true);

INSERT INTO bolsista (id, nome, senha, data_nascimento, email, curso, matricula, cpf, telefone, ativo, laboratorio_id, tipo_usuario, foto_url, cargo, bio) VALUES
('a1111111-1111-1111-1111-111111111111', 'Carlos Henrique Alencar', '$2a$10$bTT.MiXD1zXSLvIxrMQqV.YR2nTjqkSpwD6P3Cjn3XyZCamHk2BO2', '1988-04-10', 'admin@sisbolsa.com',            'Sistemas de Informação', 'ADM2024001', '012.345.678-90', '(31) 98800-0001', true, NULL,                                   'ADMIN',    'https://i.pravatar.cc/150?img=60', NULL,            NULL),
('d1111111-1111-1111-1111-111111111111', 'Lucas Oliveira',          '$2a$10$bTT.MiXD1zXSLvIxrMQqV.YR2nTjqkSpwD6P3Cjn3XyZCamHk2BO2', '2002-05-15', 'lucas.oliveira@aluno.sisbolsa.com', 'Ciência da Computação', '20221001',   '111.222.333-44', '(31) 99111-2233', true, 'c1111111-1111-1111-1111-111111111111', 'BOLSISTA', 'https://i.pravatar.cc/150?img=12', 'DESENVOLVEDOR', NULL),
('d2222222-2222-2222-2222-222222222222', 'Mariana Santos',          '$2a$10$bTT.MiXD1zXSLvIxrMQqV.YR2nTjqkSpwD6P3Cjn3XyZCamHk2BO2', '2001-11-20', 'mariana.santos@aluno.sisbolsa.com', 'Engenharia de Software', '20211002',   '222.333.444-55', '(31) 99222-3344', true, 'c1111111-1111-1111-1111-111111111111', 'BOLSISTA', 'https://i.pravatar.cc/150?img=47', 'PESQUISADOR',   NULL),
('d3333333-3333-3333-3333-333333333333', 'Diego Almeida',           '$2a$10$bTT.MiXD1zXSLvIxrMQqV.YR2nTjqkSpwD6P3Cjn3XyZCamHk2BO2', '2003-02-28', 'diego.almeida@aluno.sisbolsa.com',  'Sistemas de Informação', '20231003',   '333.444.555-66', '(31) 99333-4455', true, 'c2222222-2222-2222-2222-222222222222', 'BOLSISTA', 'https://i.pravatar.cc/150?img=33', 'DESIGNER',      NULL);

INSERT INTO projeto (id, nome, descricao, laboratorio_id, ativo) VALUES
('e1111111-1111-1111-1111-111111111111', 'Sistema Web de Gestão Acadêmica',          'Desenvolvimento de plataforma web para controle de bolsas de pesquisa e auditoria.', 'c1111111-1111-1111-1111-111111111111', true),
('e2222222-2222-2222-2222-222222222222', 'Modelagem Preditiva com Machine Learning', 'Pesquisa e implementação de algoritmos de classificação em bases de dados abertas.', 'c2222222-2222-2222-2222-222222222222', true);

INSERT INTO bolsista_projeto (bolsista_id, projeto_id) VALUES
('d1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111'),
('d2222222-2222-2222-2222-222222222222', 'e1111111-1111-1111-1111-111111111111'),
('d3333333-3333-3333-3333-333333333333', 'e2222222-2222-2222-2222-222222222222');

INSERT INTO frequencia (id, bolsista_id, data, horas_trabalhadas, descricao, ativo) VALUES
('f1111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', CURRENT_DATE - INTERVAL '2 days', 4.0, 'Implementação de autenticação JWT e testes de endpoints.', true),
('f2222222-2222-2222-2222-222222222222', 'd1111111-1111-1111-1111-111111111111', CURRENT_DATE - INTERVAL '1 day',  4.0, 'Criação dos componentes de tabela e modais no frontend React.', true),
('f3333333-3333-3333-3333-333333333333', 'd2222222-2222-2222-2222-222222222222', CURRENT_DATE - INTERVAL '2 days', 5.0, 'Revisão bibliográfica e elaboração do plano de testes da plataforma.', true),
('f4444444-4444-4444-4444-444444444444', 'd2222222-2222-2222-2222-222222222222', CURRENT_DATE,                     4.0, 'Mapeamento de casos de uso e validação de requisitos com a equipe.', true),
('f5555555-5555-5555-5555-555555555555', 'd3333333-3333-3333-3333-333333333333', CURRENT_DATE - INTERVAL '1 day',  6.0, 'Limpeza e pré-processamento do dataset para o modelo preditivo.', true);
