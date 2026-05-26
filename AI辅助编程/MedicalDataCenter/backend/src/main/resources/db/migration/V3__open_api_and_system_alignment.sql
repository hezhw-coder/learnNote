create table if not exists cdm_encounter (
    id bigint auto_increment primary key,
    patient_code varchar(128) not null,
    encounter_code varchar(128) not null,
    encounter_type varchar(64) not null,
    department_name varchar(128),
    doctor_name varchar(128),
    encounter_date varchar(32) not null,
    unique (encounter_code)
);

create table if not exists cdm_lab (
    id bigint auto_increment primary key,
    patient_code varchar(128) not null,
    encounter_code varchar(128),
    item_code varchar(64) not null,
    item_name varchar(128) not null,
    result_value varchar(64),
    unit varchar(32),
    result_flag varchar(32),
    report_date varchar(32) not null
);

insert into meta_dataset (code, name, description)
select 'cdm_encounter', 'Encounter CDM', 'Standard encounter dataset'
where not exists (select 1 from meta_dataset where code = 'cdm_encounter');

insert into meta_dataset (code, name, description)
select 'cdm_lab', 'Lab CDM', 'Standard lab dataset'
where not exists (select 1 from meta_dataset where code = 'cdm_lab');

insert ignore into meta_dataset_field (dataset_code, field_code, field_name, field_type) values
    ('cdm_encounter', 'patient_code', 'Patient Code', 'STRING'),
    ('cdm_encounter', 'encounter_code', 'Encounter Code', 'STRING'),
    ('cdm_encounter', 'encounter_type', 'Encounter Type', 'STRING'),
    ('cdm_encounter', 'department_name', 'Department Name', 'STRING'),
    ('cdm_encounter', 'doctor_name', 'Doctor Name', 'STRING'),
    ('cdm_encounter', 'encounter_date', 'Encounter Date', 'STRING'),
    ('cdm_lab', 'patient_code', 'Patient Code', 'STRING'),
    ('cdm_lab', 'encounter_code', 'Encounter Code', 'STRING'),
    ('cdm_lab', 'item_code', 'Item Code', 'STRING'),
    ('cdm_lab', 'item_name', 'Item Name', 'STRING'),
    ('cdm_lab', 'result_value', 'Result Value', 'STRING'),
    ('cdm_lab', 'unit', 'Unit', 'STRING'),
    ('cdm_lab', 'result_flag', 'Result Flag', 'STRING'),
    ('cdm_lab', 'report_date', 'Report Date', 'STRING');

insert ignore into cdm_encounter (patient_code, encounter_code, encounter_type, department_name, doctor_name, encounter_date) values
    ('P2026001', 'E2026001', '门诊', '心内科', '李医生', '2026-05-10'),
    ('P2026002', 'E2026002', '住院', '内分泌科', '王医生', '2026-05-11'),
    ('P2026003', 'E2026003', '门诊', '检验科', '赵医生', '2026-05-12');

insert ignore into cdm_lab (patient_code, encounter_code, item_code, item_name, result_value, unit, result_flag, report_date) values
    ('P2026001', 'E2026001', 'HB', '血红蛋白', '132', 'g/L', 'NORMAL', '2026-05-10'),
    ('P2026002', 'E2026002', 'GLU', '空腹血糖', '7.1', 'mmol/L', 'HIGH', '2026-05-11'),
    ('P2026003', 'E2026003', 'WBC', '白细胞计数', '6.2', '10^9/L', 'NORMAL', '2026-05-12');

insert ignore into sys_permission (code, name) values
    ('SYSTEM_USER_MANAGE', 'Manage users');

insert ignore into sys_role_permission (role_id, permission_id)
select r.id, p.id
from sys_role r
join sys_permission p on p.code = 'SYSTEM_USER_MANAGE'
where r.code = 'ADMIN';

update api_client
set scopes = 'patients.read,reports.read,labs.read,encounters.read'
where client_id = 'demo-client'
  and scopes not like '%encounters.read%';
