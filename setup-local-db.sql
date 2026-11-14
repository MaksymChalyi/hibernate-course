-- Одноразове налаштування локального PostgreSQL для курсу.
-- Виконати від імені суперкористувача postgres:
--   psql -U postgres -f setup-local-db.sql
-- або відкрити цей файл у pgAdmin / DBeaver / IntelliJ IDEA і виконати.

create user minibank with password 'minibank';
create database minibank owner minibank;

-- Таблиці й схеми модулів (m01, m02, ...) створять Hibernate і Flyway самі.
