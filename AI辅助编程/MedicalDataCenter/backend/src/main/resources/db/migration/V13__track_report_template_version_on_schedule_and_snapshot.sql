alter table report_schedule
    add column if not exists template_version int not null default 0;

alter table report_snapshot
    add column if not exists template_version int not null default 0;
