create table if not exists sys_user (
    id bigint auto_increment primary key,
    username varchar(64) not null unique,
    password varchar(255) not null,
    display_name varchar(128) not null,
    enabled boolean not null default true,
    deleted boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists sys_role (
    id bigint auto_increment primary key,
    code varchar(64) not null unique,
    name varchar(128) not null
);

create table if not exists sys_permission (
    id bigint auto_increment primary key,
    code varchar(128) not null unique,
    name varchar(128) not null
);

create table if not exists sys_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id)
);

create table if not exists sys_role_permission (
    role_id bigint not null,
    permission_id bigint not null,
    primary key (role_id, permission_id)
);

create table if not exists sys_audit_log (
    id bigint auto_increment primary key,
    actor varchar(128) not null,
    action varchar(128) not null,
    target_type varchar(64),
    target_id varchar(64),
    detail varchar(2000),
    created_at timestamp not null default current_timestamp
);

create table if not exists ds_source (
    id bigint auto_increment primary key,
    name varchar(128) not null,
    type varchar(32) not null,
    jdbc_url varchar(500),
    username varchar(128),
    database_name varchar(128),
    status varchar(32) not null,
    deleted boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists ds_source_secret (
    source_id bigint primary key,
    encrypted_password varchar(2000)
);

create table if not exists etl_job (
    id bigint auto_increment primary key,
    name varchar(128) not null,
    data_source_id bigint not null,
    load_mode varchar(32) not null,
    source_table varchar(128),
    extract_sql varchar(2000),
    increment_field varchar(128),
    id_field varchar(128),
    name_field varchar(128),
    gender_field varchar(128),
    birth_date_field varchar(128),
    status varchar(32) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists etl_job_run (
    id bigint auto_increment primary key,
    job_id bigint not null,
    status varchar(32) not null,
    extracted_count int not null default 0,
    message varchar(2000),
    started_at timestamp not null default current_timestamp,
    finished_at timestamp
);

create table if not exists etl_increment_checkpoint (
    job_id bigint primary key,
    checkpoint_value varchar(255),
    updated_at timestamp not null default current_timestamp
);

create table if not exists ods_record (
    id bigint auto_increment primary key,
    job_id bigint not null,
    batch_run_id bigint not null,
    source_table varchar(128),
    record_json longtext not null,
    created_at timestamp not null default current_timestamp
);

create table if not exists cdm_patient (
    id bigint auto_increment primary key,
    source_job_id bigint not null,
    patient_code varchar(128) not null,
    patient_name varchar(128) not null,
    gender varchar(32),
    birth_date varchar(32),
    unique (source_job_id, patient_code)
);

create table if not exists meta_dataset (
    id bigint auto_increment primary key,
    code varchar(64) not null unique,
    name varchar(128) not null,
    description varchar(512)
);

create table if not exists meta_dataset_field (
    id bigint auto_increment primary key,
    dataset_code varchar(64) not null,
    field_code varchar(64) not null,
    field_name varchar(128) not null,
    field_type varchar(32) not null
);

create table if not exists report_template (
    id bigint auto_increment primary key,
    name varchar(128) not null,
    dataset_code varchar(64) not null,
    design_json longtext not null,
    status varchar(32) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists report_snapshot (
    id bigint auto_increment primary key,
    template_id bigint not null,
    snapshot_json longtext not null,
    export_path varchar(500),
    created_at timestamp not null default current_timestamp
);

create table if not exists report_schedule (
    id bigint auto_increment primary key,
    template_id bigint not null,
    cron_expression varchar(64) not null,
    enabled boolean not null default true,
    channel varchar(32) not null default 'IN_APP'
);

create table if not exists notify_message (
    id bigint auto_increment primary key,
    title varchar(128) not null,
    content varchar(1000) not null,
    recipient varchar(64) not null,
    read_flag boolean not null default false,
    created_at timestamp not null default current_timestamp
);

create table if not exists api_client (
    id bigint auto_increment primary key,
    name varchar(128) not null,
    client_id varchar(128) not null unique,
    encrypted_secret varchar(2000) not null,
    scopes varchar(500) not null,
    enabled boolean not null default true,
    created_at timestamp not null default current_timestamp
);

create table if not exists api_scope (
    id bigint auto_increment primary key,
    code varchar(128) not null unique,
    name varchar(128) not null,
    description varchar(512)
);

create table if not exists api_access_log (
    id bigint auto_increment primary key,
    client_id varchar(128) not null,
    endpoint varchar(256) not null,
    scope varchar(256),
    status int not null,
    message varchar(500),
    created_at timestamp not null default current_timestamp
);

create table if not exists api_rate_limit_rule (
    id bigint auto_increment primary key,
    client_id varchar(128) not null,
    endpoint varchar(256) not null,
    capacity int not null,
    refill_tokens int not null,
    refill_seconds int not null
);

insert into sys_user (username, password, display_name, enabled, deleted)
select 'admin', '{noop}Admin@123', 'admin', true, false
where not exists (select 1 from sys_user where username = 'admin');

insert into sys_role (code, name)
select 'ADMIN', 'Administrator'
where not exists (select 1 from sys_role where code = 'ADMIN');

insert ignore into sys_permission (code, name) values
    ('AUTH_ME', 'View current user'),
    ('SYSTEM_USER_VIEW', 'View users'),
    ('SYSTEM_ROLE_VIEW', 'View roles'),
    ('DATASOURCE_MANAGE', 'Manage data sources'),
    ('ETL_MANAGE', 'Manage etl'),
    ('DATASET_VIEW', 'View datasets'),
    ('REPORT_MANAGE', 'Manage reports'),
    ('OPEN_API_MANAGE', 'Manage open api');

insert ignore into sys_user_role (user_id, role_id)
select u.id, r.id from sys_user u cross join sys_role r where u.username = 'admin' and r.code = 'ADMIN';

insert ignore into sys_role_permission (role_id, permission_id)
select r.id, p.id from sys_role r join sys_permission p on 1 = 1 where r.code = 'ADMIN';

insert into meta_dataset (code, name, description)
select 'cdm_patient', 'Patient CDM', 'Standard patient dataset'
where not exists (select 1 from meta_dataset where code = 'cdm_patient');

insert ignore into meta_dataset_field (dataset_code, field_code, field_name, field_type) values
    ('cdm_patient', 'patient_code', 'Patient Code', 'STRING'),
    ('cdm_patient', 'patient_name', 'Patient Name', 'STRING'),
    ('cdm_patient', 'gender', 'Gender', 'STRING'),
    ('cdm_patient', 'birth_date', 'Birth Date', 'STRING');

insert ignore into api_scope (code, name, description) values
    ('patients.read', 'Read patients', 'Query patient records'),
    ('reports.read', 'Read reports', 'Query report snapshots'),
    ('labs.read', 'Read labs', 'Query lab records'),
    ('encounters.read', 'Read encounters', 'Query encounter records');

insert into api_client (name, client_id, encrypted_secret, scopes, enabled)
select 'Default Client', 'demo-client', 'ZGVtby1zZWNyZXQ=', 'patients.read,reports.read', true
where not exists (select 1 from api_client where client_id = 'demo-client');

insert into api_rate_limit_rule (client_id, endpoint, capacity, refill_tokens, refill_seconds)
select 'demo-client', '/open-api/v1/patients', 10, 10, 60
where not exists (
    select 1 from api_rate_limit_rule where client_id = 'demo-client' and endpoint = '/open-api/v1/patients'
);
