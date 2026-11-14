-- Схема Mini Bank. Відповідає entity з пакета ua.minibank.domain.
-- Послідовності з кроком 50 — як allocationSize у @SequenceGenerator.
-- start with 1000: id нижче 1000 лишаються для даних, вставлених вручну.

create sequence customer_seq start with 1000 increment by 50;
create sequence account_seq start with 1000 increment by 50;
create sequence transaction_seq start with 1000 increment by 50;

create table customers (
    id         bigint primary key,
    first_name varchar(100) not null,
    last_name  varchar(100) not null,
    email      varchar(255) not null unique
);

create table accounts (
    id          bigint primary key,
    number      varchar(34)   not null unique,
    currency    varchar(3)    not null,
    balance     numeric(19, 4) not null,
    version     bigint        not null,
    customer_id bigint        not null references customers (id)
);

create index idx_accounts_customer on accounts (customer_id);

create table transactions (
    id          bigint primary key,
    account_id  bigint                   not null references accounts (id),
    type        varchar(10)              not null,
    amount      numeric(19, 4)           not null,
    description varchar(255)             not null,
    created_at  timestamp(6) with time zone not null
);

-- Під запит історії: where account_id = ? order by created_at desc
create index idx_transactions_account_created on transactions (account_id, created_at desc);
