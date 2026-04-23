-- Создаём схемы при первом запуске postgres.
-- Запускается автоматически через docker-entrypoint-initdb.d.
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS game;
