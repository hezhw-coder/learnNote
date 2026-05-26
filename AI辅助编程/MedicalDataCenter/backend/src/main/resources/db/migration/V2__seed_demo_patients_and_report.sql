insert ignore into cdm_patient (source_job_id, patient_code, patient_name, gender, birth_date) values
    (900001, 'P2026001', '张伟', '男', '1982-03-14'),
    (900001, 'P2026002', '李娜', '女', '1990-07-22'),
    (900001, 'P2026003', '王磊', '男', '1978-11-05'),
    (900001, 'P2026004', '赵敏', '女', '1988-01-19'),
    (900001, 'P2026005', '陈晨', '女', '1995-09-30'),
    (900001, 'P2026006', '刘洋', '男', '1985-05-08'),
    (900001, 'P2026007', '孙静', '女', '1975-12-11'),
    (900001, 'P2026008', '周凯', '男', '1992-04-26');

insert into report_template (name, dataset_code, design_json, status)
select
    '患者主数据概览报表',
    'cdm_patient',
    '{"id":"template-demo-patient-overview","name":"患者主数据概览报表","layout":{"columns":2,"gap":16},"filters":[{"field":"gender","label":"性别","value":""}],"widgets":[{"id":"widget-metric-total","type":"metric","title":"患者总数","datasetId":"cdm_patient","description":"基于内置示例患者数据生成的指标卡","config":{"field":"patient_code","aggregation":"count","color":"#409eff"}},{"id":"widget-table-detail","type":"table","title":"患者明细","datasetId":"cdm_patient","description":"展示患者编码、姓名、性别和出生日期","config":{"columns":"patient_code,patient_name,gender,birth_date","pageSize":10}},{"id":"widget-bar-gender","type":"bar","title":"患者性别分布","datasetId":"cdm_patient","description":"按性别统计示例患者数量","config":{"categoryField":"gender","valueField":"patient_code","aggregation":"count","color":"#67c23a"}},{"id":"widget-line-birth","type":"line","title":"患者出生日期趋势","datasetId":"cdm_patient","description":"按出生日期查看样例患者分布","config":{"xField":"birth_date","yField":"patient_code","aggregation":"count","color":"#e6a23c"}}]}',
    'PUBLISHED'
where not exists (
    select 1
    from report_template
    where name = '患者主数据概览报表'
      and dataset_code = 'cdm_patient'
);
