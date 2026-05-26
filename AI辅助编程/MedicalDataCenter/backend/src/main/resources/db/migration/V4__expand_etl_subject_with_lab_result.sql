-- Promote the ETL lab-result expansion to V4 so merged branches keep a single linear Flyway history.
alter table etl_job
    add column if not exists target_dataset_code varchar(64) not null default 'cdm_patient';

alter table etl_job
    add column if not exists extra_code_field varchar(128);

alter table etl_job
    add column if not exists value_field varchar(128);

alter table etl_job
    add column if not exists unit_field varchar(128);

alter table etl_job
    add column if not exists event_time_field varchar(128);

update etl_job
set target_dataset_code = 'cdm_patient'
where target_dataset_code is null or target_dataset_code = '';

create table if not exists cdm_lab_result (
    id bigint auto_increment primary key,
    source_job_id bigint not null,
    patient_code varchar(128) not null,
    item_code varchar(128) not null,
    item_name varchar(128) not null,
    result_value varchar(128),
    result_unit varchar(64),
    sample_time varchar(32) not null default '',
    unique (source_job_id, patient_code, item_code, sample_time)
);

insert into meta_dataset (code, name, description)
select 'cdm_lab_result', 'Lab Result CDM', 'Standard laboratory result dataset'
where not exists (select 1 from meta_dataset where code = 'cdm_lab_result');

insert ignore into meta_dataset_field (dataset_code, field_code, field_name, field_type) values
    ('cdm_lab_result', 'patient_code', 'Patient Code', 'STRING'),
    ('cdm_lab_result', 'item_code', 'Lab Item Code', 'STRING'),
    ('cdm_lab_result', 'item_name', 'Lab Item Name', 'STRING'),
    ('cdm_lab_result', 'result_value', 'Result Value', 'STRING'),
    ('cdm_lab_result', 'result_unit', 'Result Unit', 'STRING'),
    ('cdm_lab_result', 'sample_time', 'Sample Time', 'STRING');

insert ignore into cdm_lab_result (source_job_id, patient_code, item_code, item_name, result_value, result_unit, sample_time) values
    (900002, 'P2026001', 'HB', '血红蛋白', '132', 'g/L', '2026-05-20 08:00:00'),
    (900002, 'P2026001', 'WBC', '白细胞', '6.1', '10^9/L', '2026-05-20 08:00:00'),
    (900002, 'P2026002', 'GLU', '空腹血糖', '5.6', 'mmol/L', '2026-05-21 07:45:00'),
    (900002, 'P2026003', 'ALT', '谷丙转氨酶', '28', 'U/L', '2026-05-21 09:10:00'),
    (900002, 'P2026004', 'CRP', 'C反应蛋白', '3.2', 'mg/L', '2026-05-22 10:30:00'),
    (900002, 'P2026005', 'CREA', '肌酐', '79', 'umol/L', '2026-05-22 11:05:00');

insert into report_template (name, dataset_code, design_json, status)
select
    '检验结果样例预览报表',
    'cdm_lab_result',
    '{"id":"template-demo-lab-overview","name":"检验结果样例预览报表","layout":{"columns":2,"gap":16},"filters":[{"field":"item_name","label":"检验项目","value":""}],"widgets":[{"id":"widget-metric-total","type":"metric","title":"检验记录数","datasetId":"cdm_lab_result","description":"基于内置示例检验结果数据生成的指标卡","config":{"field":"item_code","aggregation":"count","color":"#409eff"}},{"id":"widget-table-detail","type":"table","title":"检验结果明细","datasetId":"cdm_lab_result","description":"展示患者编码、项目编码、项目名称、结果值、单位和采样时间","config":{"columns":"patient_code,item_code,item_name,result_value,result_unit,sample_time","pageSize":10}},{"id":"widget-bar-item","type":"bar","title":"检验项目分布","datasetId":"cdm_lab_result","description":"按检验项目统计示例记录数量","config":{"categoryField":"item_name","valueField":"item_code","aggregation":"count","color":"#67c23a"}},{"id":"widget-line-sample-time","type":"line","title":"采样时间分布","datasetId":"cdm_lab_result","description":"按采样时间查看样例检验结果分布","config":{"xField":"sample_time","yField":"item_code","aggregation":"count","color":"#e6a23c"}}]}',
    'PUBLISHED'
where not exists (
    select 1
    from report_template
    where name = '检验结果样例预览报表'
      and dataset_code = 'cdm_lab_result'
);
