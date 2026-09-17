-- dados iniciais: 3 professores, 3 laboratorios, 6 projetos, 1 admin e 7 bolsistas
-- senha de todos os usuarios: 12345678 (sha-256, vira bcrypt na etapa 3)

INSERT INTO professor (nome, email, senha, ativo, foto_url, bio) VALUES
('Dr. Roberto Mendes',
 'roberto.mendes@sisbolsa.com',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 true,
 'https://i.pravatar.cc/150?img=11',
 'Doutor em Ciencia da Computacao pela USP. Coordena o Laboratorio de Desenvolvimento de Software com foco em engenharia de software e sistemas distribuidos. Possui mais de 15 anos de experiencia em pesquisa e orientacao de bolsistas de iniciacao cientifica.'),

('Dra. Carla Souza',
 'carla.souza@sisbolsa.com',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 true,
 'https://i.pravatar.cc/150?img=49',
 'Doutora em Biologia Molecular pela UNICAMP. Coordenadora do Laboratorio de Ciencias Biologicas, com pesquisas voltadas a microbiologia ambiental e biotecnologia. Bolsista de produtividade do CNPq desde 2018.'),

('Dr. Felipe Andrade',
 'felipe.andrade@sisbolsa.com',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 true,
 'https://i.pravatar.cc/150?img=33',
 'Doutor em Engenharia Mecanica pela UFMG. Coordena o Laboratorio de Engenharia Mecatronica, com atuacao em robotica industrial, automacao e sistemas embarcados. Responsavel por tres patentes de dispositivos mecanicos registradas no INPI.');

-- ============================================================
-- Dados Iniciais - Laboratorios (3 labs, um por professor)
-- ============================================================
INSERT INTO laboratorio (nome, area_pesquisa, status, capacidade, coordenador_id, ativo) VALUES
('Lab de Desenvolvimento de Software', 'Computacao',  'Ativo', 8, 1, true),
('Lab de Ciencias Biologicas',         'Biologia',    'Ativo', 6, 2, true),
('Lab de Engenharia Mecatronica',      'Engenharia',  'Ativo', 7, 3, true);

-- ============================================================
-- Dados Iniciais - Projetos (2 por laboratorio)
-- ============================================================
INSERT INTO projeto (nome, descricao, laboratorio_id, ativo) VALUES
-- Lab 1 - Desenvolvimento de Software
('Sistema de Gestao Academica',
 'Desenvolvimento de um sistema web para gerenciamento de discentes, bolsas de pesquisa e frequencia. Utiliza Spring MVC, JDBC e PostgreSQL com foco em seguranca e usabilidade.',
 1, true),

('API de Integracao de Dados Educacionais',
 'Criacao de uma API RESTful para integracao entre sistemas academicos da instituicao, permitindo sincronizacao de matriculas, notas e frequencias com plataformas externas.',
 1, true),

-- Lab 2 - Ciencias Biologicas
('Analise Microbiologica de Solos Agricolas',
 'Mapeamento da diversidade bacteriana em solos de diferentes regioes do Cerrado utilizando sequenciamento de RNA ribossomal 16S para identificacao de especies potencialmente uteis na agricultura.',
 2, true),

('Cultivo de Microalgas para Biocombustivel',
 'Otimizacao de parametros de cultivo fotoautotrof de microalgas em fotobiorreatores para maximizacao da producao de lipideos utilizaveis como materia-prima para biodiesel.',
 2, true),

-- Lab 3 - Engenharia Mecatronica
('Robo de Inspecao Industrial',
 'Projeto e construcao de um robo movel autonomo equipado com sensores infravermelho e camera para inspecao de tubulacoes e estruturas de dificil acesso em plantas industriais.',
 3, true),

('Automacao de Linha de Montagem',
 'Desenvolvimento de um sistema de controle CLP para automacao de uma linha de montagem didatica, integrando sensores, atuadores e interface supervisoria SCADA.',
 3, true);

-- ============================================================
-- Dados Iniciais - Admin e Bolsistas
-- todos os campos preenchidos, senha 12345678
-- ============================================================
INSERT INTO bolsista (nome, senha, data_nascimento, email, curso, matricula, cpf, telefone, ativo, laboratorio_id, tipo_usuario, foto_url, cargo, bio) VALUES

-- Admin (ID 1)
('Carlos Henrique Alencar',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 '1988-04-10',
 'admin@sisbolsa.com',
 'Sistemas de Informacao',
 'ADM2024001',
 '012.345.678-90',
 '(31) 98800-0001',
 true, NULL, 'ADMIN',
 'https://i.pravatar.cc/150?img=60',
 NULL,
 'Administrador geral da plataforma SisBolsa. Responsavel pela gestao de usuarios, laboratorios e auditoria dos dados do sistema. Graduado em Sistemas de Informacao com especializacao em seguranca da informacao.'),

-- Bolsistas do Lab 1 - Desenvolvimento de Software (IDs 2, 3, 4)
('Thiago Rocha',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 '2001-08-14',
 'thiago.rocha@aluno.sisbolsa.com',
 'Ciencia da Computacao',
 '2021102301',
 '123.456.789-01',
 '(31) 98811-0001',
 true, 1, 'BOLSISTA',
 'https://i.pravatar.cc/150?img=12',
 'DESENVOLVEDOR',
 'Bolsista de iniciacao cientifica no Laboratorio de Desenvolvimento de Software. Atua no desenvolvimento backend do sistema academico usando Java e Spring. Tem interesse em arquitetura de microsservicos e integracao continua.'),

('Camila Pires',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 '2002-03-22',
 'camila.pires@aluno.sisbolsa.com',
 'Engenharia de Software',
 '2022102302',
 '234.567.890-12',
 '(31) 98811-0002',
 true, 1, 'BOLSISTA',
 'https://i.pravatar.cc/150?img=43',
 'PESQUISADOR',
 'Bolsista pesquisadora no Lab de Desenvolvimento de Software. Responsavel pelo levantamento de requisitos e testes de aceitacao do sistema de gestao academica. Possui certificacao ISTQB Foundation Level.'),

('Leonardo Farias',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 '2000-11-05',
 'leonardo.farias@aluno.sisbolsa.com',
 'Sistemas de Informacao',
 '2020102303',
 '345.678.901-23',
 '(31) 98811-0003',
 true, 1, 'BOLSISTA',
 'https://i.pravatar.cc/150?img=18',
 'LIDER_TECNICO',
 'Lider tecnico da equipe de desenvolvimento do Lab de Software. Coordena as sprints, realiza revisoes de codigo e garante a qualidade das entregas. Experiencia previa em estagio em empresa de tecnologia de Belo Horizonte.'),

-- Bolsistas do Lab 2 - Ciencias Biologicas (IDs 5, 6)
('Diego Almeida',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 '2003-06-18',
 'diego.almeida@aluno.sisbolsa.com',
 'Biologia',
 '2023104501',
 '456.789.012-34',
 '(31) 98822-0001',
 true, 2, 'BOLSISTA',
 'https://i.pravatar.cc/150?img=52',
 'AUXILIAR',
 'Auxiliar de laboratorio no Lab de Ciencias Biologicas. Responsavel pela preparacao de meios de cultura, esterilizacao de vidraria e manutencao do estoque de reagentes. Ingressou na bolsa no primeiro semestre de 2023.'),

('Larissa Moura',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 '2001-09-30',
 'larissa.moura@aluno.sisbolsa.com',
 'Biomedicina',
 '2021104502',
 '567.890.123-45',
 '(31) 98822-0002',
 true, 2, 'BOLSISTA',
 'https://i.pravatar.cc/150?img=28',
 'PESQUISADOR',
 'Pesquisadora no Laboratorio de Ciencias Biologicas com foco em microbiologia ambiental. Realiza coleta e analise de amostras de solo do Cerrado e contribui na redacao de artigos cientificos do grupo de pesquisa.'),

-- Bolsistas do Lab 3 - Engenharia Mecatronica (IDs 7, 8)
('Bruno Carvalho',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 '2000-02-25',
 'bruno.carvalho@aluno.sisbolsa.com',
 'Engenharia Mecanica',
 '2020106701',
 '678.901.234-56',
 '(31) 98833-0001',
 true, 3, 'BOLSISTA',
 'https://i.pravatar.cc/150?img=59',
 'LIDER_TECNICO',
 'Lider tecnico do Lab de Engenharia Mecatronica. Coordena o projeto do robo de inspecao industrial, desde o design mecanico ate a integracao dos sensores. Possui experiencia em fabricacao por impressao 3D e usinagem CNC.'),

('Natalia Gomes',
 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f',
 '2002-07-11',
 'natalia.gomes@aluno.sisbolsa.com',
 'Engenharia de Controle e Automacao',
 '2022106702',
 '789.012.345-67',
 '(31) 98833-0002',
 true, 3, 'BOLSISTA',
 'https://i.pravatar.cc/150?img=20',
 'DESENVOLVEDOR',
 'Bolsista desenvolvedora no Lab de Mecatronica. Atua na programacao de CLPs e no desenvolvimento da interface supervisoria SCADA para a linha de montagem automatizada. Tem conhecimento em linguagens Ladder e C.');

-- ============================================================
-- Vinculos Bolsista x Projeto (cada bolsista em pelo menos um projeto do seu lab)
-- ============================================================
INSERT INTO bolsista_projeto (bolsista_id, projeto_id) VALUES
-- Lab 1: Thiago(2), Camila(3), Leonardo(4) nos projetos 1 e 2
(2, 1),
(3, 1),
(4, 2),
(2, 2),

-- Lab 2: Diego(5), Larissa(6) nos projetos 3 e 4
(5, 3),
(6, 3),
(6, 4),

-- Lab 3: Bruno(7), Natalia(8) nos projetos 5 e 6
(7, 5),
(8, 6),
(7, 6);

-- ============================================================
-- Frequencias (todos os bolsistas com historico preenchido)
-- ============================================================
INSERT INTO frequencia (bolsista_id, data, horas_trabalhadas, descricao, ativo) VALUES

-- Thiago Rocha (ID 2) - Lab Software
(2, CURRENT_DATE - INTERVAL '1 day',  4.0, 'Implementacao dos endpoints REST para cadastro e edicao de bolsistas no sistema academico.', true),
(2, CURRENT_DATE - INTERVAL '3 days', 4.0, 'Configuracao do pipeline de integracao continua com GitHub Actions e cobertura de testes JUnit.', true),
(2, CURRENT_DATE - INTERVAL '7 days', 3.5, 'Reuniao de alinhamento com o coordenador e revisao dos requisitos para o proximo sprint.', true),
(2, CURRENT_DATE - INTERVAL '10 days',4.0, 'Desenvolvimento do modulo de autenticacao com hash SHA-256 e controle de sessao HTTP.', true),

-- Camila Pires (ID 3) - Lab Software
(3, CURRENT_DATE - INTERVAL '2 days', 3.0, 'Elaboracao de casos de teste para os fluxos de login e cadastro de usuarios.', true),
(3, CURRENT_DATE - INTERVAL '4 days', 3.0, 'Execucao de testes de aceitacao com usuarios reais e registro de defeitos encontrados.', true),
(3, CURRENT_DATE - INTERVAL '8 days', 3.5, 'Levantamento de requisitos nao funcionais de desempenho e acessibilidade do sistema.', true),
(3, CURRENT_DATE - INTERVAL '12 days',4.0, 'Documentacao das historias de usuario no backlog e priorizacao com o time de desenvolvimento.', true),

-- Leonardo Farias (ID 4) - Lab Software
(4, CURRENT_DATE - INTERVAL '1 day',  5.0, 'Revisao de pull requests do time e aprovacao das mudancas no modulo de frequencias.', true),
(4, CURRENT_DATE - INTERVAL '3 days', 5.0, 'Planejamento do sprint 4 com divisao de tarefas e estimativas em story points.', true),
(4, CURRENT_DATE - INTERVAL '6 days', 4.5, 'Refatoracao do modulo de relatorios para eliminar consultas N+1 ao banco de dados.', true),
(4, CURRENT_DATE - INTERVAL '9 days', 4.0, 'Apresentacao do progresso do projeto para o professor coordenador e ajuste de cronograma.', true),

-- Diego Almeida (ID 5) - Lab Biologicas
(5, CURRENT_DATE - INTERVAL '2 days', 4.0, 'Preparacao de 20 placas de Petri com meio agar LB para cultivo das amostras coletadas.', true),
(5, CURRENT_DATE - INTERVAL '4 days', 4.0, 'Esterilizacao de vidraria e verificacao do estoque de reagentes para o protocolo de extracao.', true),
(5, CURRENT_DATE - INTERVAL '7 days', 3.5, 'Auxilio na coleta de amostras de solo em tres pontos distintos da area de estudo no Cerrado.', true),
(5, CURRENT_DATE - INTERVAL '11 days',4.0, 'Limpeza e organizacao do laboratorio apos protocolo de extracao de DNA genomico.', true),

-- Larissa Moura (ID 6) - Lab Biologicas
(6, CURRENT_DATE - INTERVAL '1 day',  5.0, 'Analise bioinformatica das sequencias 16S obtidas no sequenciamento de nova geracao.', true),
(6, CURRENT_DATE - INTERVAL '3 days', 5.0, 'Execucao do protocolo de PCR para amplificacao da regiao V3-V4 do gene 16S rRNA.', true),
(6, CURRENT_DATE - INTERVAL '6 days', 4.5, 'Extracao de DNA total das amostras de solo usando kit de extracao Qiagen DNeasy.', true),
(6, CURRENT_DATE - INTERVAL '9 days', 5.0, 'Redacao da secao de metodologia do artigo cientifico para submissao na revista PLOS ONE.', true),

-- Bruno Carvalho (ID 7) - Lab Mecatronica
(7, CURRENT_DATE - INTERVAL '2 days', 6.0, 'Montagem e ajuste fino do chassis do robo de inspecao com fixacao dos motores de passo.', true),
(7, CURRENT_DATE - INTERVAL '4 days', 6.0, 'Modelagem 3D no SolidWorks do suporte de sensores infravermelho e envio para impressao.', true),
(7, CURRENT_DATE - INTERVAL '7 days', 5.0, 'Reuniao tecnica com parceiro industrial para alinhar requisitos de inspecao das tubulacoes.', true),
(7, CURRENT_DATE - INTERVAL '10 days',5.5, 'Testes de resistencia estrutural do chassis em ambiente simulado de vibracao industrial.', true),

-- Natalia Gomes (ID 8) - Lab Mecatronica
(8, CURRENT_DATE - INTERVAL '1 day',  5.0, 'Programacao da logica de controle da linha de montagem em linguagem Ladder no CLP Siemens S7.', true),
(8, CURRENT_DATE - INTERVAL '3 days', 5.0, 'Configuracao da interface SCADA no WinCC para monitoramento em tempo real dos atuadores.', true),
(8, CURRENT_DATE - INTERVAL '5 days', 4.5, 'Integracao e teste do sensor fotoeletrico de contagem de pecas na esteira transportadora.', true),
(8, CURRENT_DATE - INTERVAL '8 days', 4.0, 'Elaboracao do manual de operacao do sistema de automacao para uso pelos tecnicos do lab.', true);
