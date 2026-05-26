create table if not exists sys_parameter (
    param_key varchar(128) primary key,
    param_value varchar(512) not null,
    description varchar(255) not null,
    updated_by varchar(64) not null default 'system',
    updated_at timestamp not null default current_timestamp
);

insert into sys_parameter (param_key, param_value, description, updated_by)
select 'app.security.issuer', 'medical-data-center', 'JWT 签发者', 'system'
where not exists (select 1 from sys_parameter where param_key = 'app.security.issuer');

insert into sys_parameter (param_key, param_value, description, updated_by)
select 'app.rate-limit.default-capacity', '30', '默认限流桶容量', 'system'
where not exists (select 1 from sys_parameter where param_key = 'app.rate-limit.default-capacity');

insert into sys_parameter (param_key, param_value, description, updated_by)
select 'app.export.dir', './data/exports', '报表导出目录', 'system'
where not exists (select 1 from sys_parameter where param_key = 'app.export.dir');

insert into sys_permission (code, name)
select 'SYSTEM_ROLE_MANAGE', 'Manage roles'
where not exists (select 1 from sys_permission where code = 'SYSTEM_ROLE_MANAGE');

insert into sys_permission (code, name)
select 'SYSTEM_PARAM_MANAGE', 'Manage system parameters'
where not exists (select 1 from sys_permission where code = 'SYSTEM_PARAM_MANAGE');

insert into sys_role_permission (role_id, permission_id)
select r.id, p.id
from sys_role r
join sys_permission p on p.code = 'SYSTEM_ROLE_MANAGE'
where r.code = 'ADMIN'
  and not exists (
      select 1
      from sys_role_permission rp
      where rp.role_id = r.id
        and rp.permission_id = p.id
  );

insert into sys_role_permission (role_id, permission_id)
select r.id, p.id
from sys_role r
join sys_permission p on p.code = 'SYSTEM_PARAM_MANAGE'
where r.code = 'ADMIN'
  and not exists (
      select 1
      from sys_role_permission rp
      where rp.role_id = r.id
        and rp.permission_id = p.id
  );
