create table if not exists error_log (
    id binary(16) not null primary key,
    type varchar(255),
    message varchar(2000),
    path varchar(255),
    created_at datetime(6)
);