alter table report_schedule
    add column if not exists runtime_params_json varchar(4000);

alter table report_snapshot
    add column if not exists schedule_id bigint,
    add column if not exists runtime_params_json varchar(4000);

