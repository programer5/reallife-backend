create table if not exists ops_alert_log (
    id binary(16) not null primary key,
    channel varchar(30) not null,
    alert_key varchar(190),
    title varchar(255) not null,
    body text,
    level varchar(30) not null,
    status varchar(30) not null,
    requested_by varchar(120),
    created_at datetime(6) not null
    );

create index idx_ops_alert_log_created_at
    on ops_alert_log (created_at desc);

create index idx_ops_alert_log_channel_created_at
    on ops_alert_log (channel, created_at desc);

create index idx_ops_alert_log_status_created_at
    on ops_alert_log (status, created_at desc);