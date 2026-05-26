alter table report_schedule
    add column if not exists owner varchar(64) not null default 'admin';

update report_schedule
set owner = 'admin'
where owner is null or owner = '';
