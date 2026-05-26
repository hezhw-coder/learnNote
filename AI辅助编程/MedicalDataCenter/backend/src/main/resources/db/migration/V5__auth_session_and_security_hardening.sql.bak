alter table sys_user add column if not exists failed_login_attempts int not null default 0;
alter table sys_user add column if not exists locked_until timestamp null;
alter table sys_user add column if not exists last_login_at timestamp null;
alter table sys_user add column if not exists password_changed_at timestamp null;

update sys_user
set failed_login_attempts = coalesce(failed_login_attempts, 0)
where failed_login_attempts is null;

create table if not exists auth_session (
    session_id varchar(64) primary key,
    user_id bigint not null,
    username varchar(64) not null,
    refresh_token_hash varchar(128) not null,
    refresh_expires_at timestamp not null,
    revoked boolean not null default false,
    revoked_at timestamp null,
    revoked_reason varchar(255),
    last_refreshed_at timestamp not null default current_timestamp,
    created_at timestamp not null default current_timestamp
);

create index if not exists idx_auth_session_user_id on auth_session(user_id);
create index if not exists idx_auth_session_username on auth_session(username);

insert into sys_parameter (param_key, param_value, description, updated_by)
select 'app.security.password-min-length', '8', '平台账号最小密码长度', 'system'
where not exists (select 1 from sys_parameter where param_key = 'app.security.password-min-length');

insert into sys_parameter (param_key, param_value, description, updated_by)
select 'app.security.max-failed-attempts', '5', '连续失败触发锁定阈值', 'system'
where not exists (select 1 from sys_parameter where param_key = 'app.security.max-failed-attempts');

insert into sys_parameter (param_key, param_value, description, updated_by)
select 'app.security.lock-minutes', '15', '账号锁定时长（分钟）', 'system'
where not exists (select 1 from sys_parameter where param_key = 'app.security.lock-minutes');
