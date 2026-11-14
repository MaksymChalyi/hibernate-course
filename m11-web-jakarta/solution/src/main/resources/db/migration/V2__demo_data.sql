-- Демо-дані для навчання. У реальному проєкті тестові дані не кладуть у production-міграції:
-- їх виносять в окрему локацію Flyway, яку підключають лише локально.

insert into customers (id, first_name, last_name, email) values
    (1, 'Olena', 'Koval', 'olena@example.com'),
    (2, 'Ivan', 'Petrenko', 'ivan@example.com');

insert into accounts (id, number, currency, balance, version, customer_id) values
    (1, 'UA01', 'UAH', 10000, 0, 1),
    (2, 'UA02', 'USD', 500, 0, 1),
    (3, 'UA03', 'UAH', 3000, 0, 2);

insert into transactions (id, account_id, type, amount, description, created_at) values
    (1, 1, 'CREDIT', 10000, 'Початковий баланс', '2026-01-05 10:00:00+02'),
    (2, 2, 'CREDIT', 500, 'Початковий баланс', '2026-01-05 10:00:00+02'),
    (3, 3, 'CREDIT', 3000, 'Початковий баланс', '2026-01-05 10:00:00+02');
