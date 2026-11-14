-- Імпортовані рядки зовнішніх виписок

create sequence statement_line_seq start with 1 increment by 50;

create table statement_lines (
    id             bigint primary key,
    account_number varchar(34)    not null,
    booking_date   date           not null,
    amount         numeric(19, 4) not null,
    currency       varchar(3)     not null,
    description    varchar(255)   not null
);

create index idx_statement_lines_account_date on statement_lines (account_number, booking_date);
