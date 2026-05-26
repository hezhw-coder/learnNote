alter table etl_job
    add column if not exists field_bindings_json varchar(4000);

