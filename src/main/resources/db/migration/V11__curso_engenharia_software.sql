-- ============================================================
-- V11__curso_engenharia_software.sql
-- A seed da V5 cadastra a bolsista Mariana Santos no curso
-- 'Engenharia de Software', mas a lista da V9 nao tinha esse curso:
-- o dado existia na bolsista e sumia do dropdown de cursos.
-- ON CONFLICT porque um admin pode ja ter criado o curso pela API.
-- ============================================================

INSERT INTO curso (nome) VALUES ('Engenharia de Software')
ON CONFLICT (nome) DO NOTHING;
