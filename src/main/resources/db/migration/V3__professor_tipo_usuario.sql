-- professor nao tinha coluna de tipo, entao a heranca de Usuario nao mapeava
-- direto em jpa. com a coluna, bolsista e professor compartilham o mesmo
-- MappedSuperclass sem gambiarra de campo transiente.
ALTER TABLE professor ADD COLUMN tipo_usuario VARCHAR(20) NOT NULL DEFAULT 'PROFESSOR';
