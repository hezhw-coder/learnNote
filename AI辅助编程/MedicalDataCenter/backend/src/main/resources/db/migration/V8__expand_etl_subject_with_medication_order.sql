create table if not exists cdm_medication_order (
    id bigint auto_increment primary key,
    source_job_id bigint not null,
    patient_code varchar(128) not null,
    drug_code varchar(128) not null,
    drug_name varchar(128) not null,
    dose_value varchar(64),
    dose_unit varchar(32),
    order_time varchar(32) not null default '',
    unique (source_job_id, patient_code, drug_code, order_time)
);

insert into meta_dataset (code, name, description)
select 'cdm_medication_order', 'Medication Order CDM', 'Standard medication order dataset'
where not exists (select 1 from meta_dataset where code = 'cdm_medication_order');

insert ignore into meta_dataset_field (dataset_code, field_code, field_name, field_type) values
    ('cdm_medication_order', 'patient_code', 'Patient Code', 'STRING'),
    ('cdm_medication_order', 'drug_code', 'Drug Code', 'STRING'),
    ('cdm_medication_order', 'drug_name', 'Drug Name', 'STRING'),
    ('cdm_medication_order', 'dose_value', 'Dose Value', 'STRING'),
    ('cdm_medication_order', 'dose_unit', 'Dose Unit', 'STRING'),
    ('cdm_medication_order', 'order_time', 'Order Time', 'STRING');

insert ignore into cdm_medication_order (source_job_id, patient_code, drug_code, drug_name, dose_value, dose_unit, order_time) values
    (900003, 'P2026001', 'ASP100', '阿司匹林肠溶片', '100', 'mg', '2026-05-23 08:00:00'),
    (900003, 'P2026002', 'MET500', '二甲双胍片', '500', 'mg', '2026-05-23 08:30:00'),
    (900003, 'P2026003', 'AMO250', '阿莫西林胶囊', '250', 'mg', '2026-05-23 09:15:00'),
    (900003, 'P2026004', 'IRB150', '厄贝沙坦片', '150', 'mg', '2026-05-23 10:10:00'),
    (900003, 'P2026005', 'ROS10', '瑞舒伐他汀钙片', '10', 'mg', '2026-05-23 20:00:00'),
    (900003, 'P2026006', 'INS12', '门冬胰岛素注射液', '12', 'U', '2026-05-24 07:20:00');

insert into report_template (name, dataset_code, design_json, status)
select
    '用药医嘱样例预览报表',
    'cdm_medication_order',
    '{"id":"template-demo-medication-order-overview","name":"用药医嘱样例预览报表","layout":{"columns":2,"gap":16},"filters":[{"field":"drug_name","label":"药品名称","value":""}],"widgets":[{"id":"widget-metric-total","type":"metric","title":"医嘱记录数","datasetId":"cdm_medication_order","description":"基于内置示例用药医嘱数据生成的指标卡","config":{"field":"drug_code","aggregation":"count","color":"#409eff"}},{"id":"widget-table-detail","type":"table","title":"用药医嘱明细","datasetId":"cdm_medication_order","description":"展示患者编码、药品编码、药品名称、剂量、单位和开立时间","config":{"columns":"patient_code,drug_code,drug_name,dose_value,dose_unit,order_time","pageSize":10}},{"id":"widget-bar-drug","type":"bar","title":"药品分布","datasetId":"cdm_medication_order","description":"按药品名称统计示例记录数量","config":{"categoryField":"drug_name","valueField":"drug_code","aggregation":"count","color":"#67c23a"}},{"id":"widget-line-order-time","type":"line","title":"开立时间分布","datasetId":"cdm_medication_order","description":"按开立时间查看样例用药医嘱分布","config":{"xField":"order_time","yField":"drug_code","aggregation":"count","color":"#e6a23c"}}]}',
    'PUBLISHED'
where not exists (
    select 1
    from report_template
    where name = '用药医嘱样例预览报表'
      and dataset_code = 'cdm_medication_order'
);
