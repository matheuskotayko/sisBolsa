-- ============================================================
-- V13__remover_auditoria.sql
-- Remove a trilha de auditoria: a aplicacao nao grava nem le mais esses logs.
-- Os indices caem junto com a tabela.
-- ============================================================

DROP TABLE IF EXISTS auditoria;
