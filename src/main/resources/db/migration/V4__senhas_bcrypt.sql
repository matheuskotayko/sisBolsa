-- troca o sha-256 sem salt do seed por bcrypt.
-- sha-256 puro nao serve para senha: e rapido demais e, sem salt, a mesma
-- senha vira sempre o mesmo hash - da para quebrar com rainbow table.
--
-- a senha continua sendo 12345678 para todos os usuarios do seed.
UPDATE bolsista
   SET senha = '$2a$10$bTT.MiXD1zXSLvIxrMQqV.YR2nTjqkSpwD6P3Cjn3XyZCamHk2BO2'
 WHERE senha = 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f';

UPDATE professor
   SET senha = '$2a$10$bTT.MiXD1zXSLvIxrMQqV.YR2nTjqkSpwD6P3Cjn3XyZCamHk2BO2'
 WHERE senha = 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f';
