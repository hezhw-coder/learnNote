alter table sys_audit_log
    add column if not exists request_id varchar(128) null,
    add column if not exists remote_ip varchar(128) null,
    add column if not exists user_agent varchar(255) null,
    add column if not exists status_code int null,
    add column if not exists duration_ms bigint null;

alter table api_access_log
    add column if not exists request_id varchar(128) null,
    add column if not exists remote_ip varchar(128) null,
    add column if not exists user_agent varchar(255) null,
    add column if not exists duration_ms bigint null;

create index if not exists idx_sys_audit_log_created_at on sys_audit_log(created_at);
create index if not exists idx_sys_audit_log_request_id on sys_audit_log(request_id);
create index if not exists idx_api_access_log_created_at on api_access_log(created_at);
create index if not exists idx_api_access_log_request_id on api_access_log(request_id);
